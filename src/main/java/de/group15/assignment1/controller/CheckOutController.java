package de.group15.assignment1.controller;


import de.group15.assignment1.model.Order;
import de.group15.assignment1.model.OrderItem;
import de.group15.assignment1.model.User;
import de.group15.assignment1.repository.OrderRepository;
import de.group15.assignment1.service.BeverageService;
import de.group15.assignment1.service.ShoppingCartService;
import de.group15.assignment1.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.naming.AuthenticationException;
import java.util.List;

@Slf4j
@Controller
@RequestMapping(value = "/shoppingcart/checkout")
public class CheckOutController {
    //get " show a review of the order with all the items selected + the address"
    //post " save the items as list of orderItems inside an order object into the database then redirect to success message

    private final OrderRepository orderRepository;
    @Autowired
    ShoppingCartService shoppingCartService;
    @Autowired
    UserService userService;
    @Autowired
    BeverageService beverageService;

    public CheckOutController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @GetMapping(value = "/{orderid}")
    @PreAuthorize("isAuthenticated()")
    public String finalReviewOfSelectedItems(@PathVariable Long orderid, Model model) {
        log.info("User finished purchase");
        Order selectedOrder = new Order();
        if (this.orderRepository.findById(orderid).isPresent()) {
            selectedOrder = this.orderRepository.findById(orderid).get();

            try {
                Authentication user = userService.getCurrentUser();
                // Only the user itself can checkout its order
                if (user.getName().equals(selectedOrder.getCustomer().getUsername())) {
                    model.addAttribute("orderID", orderid);
                    model.addAttribute("orderPrice", selectedOrder.getPrice());
                    model.addAttribute("orderCustomer", selectedOrder.getCustomer().getUsername());
                    model.addAttribute("orderItems", selectedOrder.getItems());

                    return "checkout";
                }
                else {
                    model.addAttribute("message", "The requested order doesn't belong the logged in user");
                    return "error";
                }
            } catch (AuthenticationException e) {
                e.printStackTrace();
            }
        }
        model.addAttribute("message", "The requested order does not exist.");
        return "error";
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public String completePurchase(Model model) {
        log.info("User confirmed the order");
        Order newOrder = new Order();
        try {
            Authentication auth = userService.getCurrentUser();
            String id = auth.getName();
            User orderingUser = userService.getUser(id);
            List<OrderItem> listOfItems = shoppingCartService.getItemsInCart();

            log.info("System saving the order of the user .. ");
            newOrder.setCustomer(orderingUser);
            listOfItems.stream().forEach(item -> newOrder.addOrderItem(item));
            double totalPrice = newOrder.priceTotal(listOfItems);
            newOrder.setPrice(totalPrice);
            this.orderRepository.save(newOrder);
            log.info("Saving complete!");

            /******* Updating the quantity of beverages in stock after completing purchase *******/
            int orderedQuantity;
            int beverageQuantity;
            Long beverageID;
            for (int i = 0; i < listOfItems.size(); i++) {
                log.info("updating quantity in of beverage iteration number: " + i + " ");
                orderedQuantity = listOfItems.get(i).getQuantity();
                beverageQuantity = listOfItems.get(i).getBeverage().getInStock();
                beverageID = listOfItems.get(i).getBeverage().getId();
                beverageService.updateBeverageQuantity(beverageID, (beverageQuantity - orderedQuantity));
            }
            /************************************************************************************/

            shoppingCartService.clearAllItems();
            log.info("shopping cart clear of the items");
            return "redirect:/shoppingcart/checkout/" + newOrder.getId();

        } catch (AuthenticationException e) {
            e.printStackTrace();
        }
        model.addAttribute("message", "Please log in.");
        return "error";
    }

}


