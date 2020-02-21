package com.example.online_store_back_end;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.*;


@Entity
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private Long id;
    private Date date;
    private Integer totalPrice;
    private String discount;

    @ManyToMany
    Set<Product> products;

    @ElementCollection
    @OrderColumn(name="quantities_id")
    private Set<String> quantities = new HashSet<>();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name="customer_id")
    private User customer;

    public Purchase() { };

    public Purchase(Set<Product> products,Set<String> quantities ,String discount,Integer totalPrice,User user, Date date) {
        user.addPurchase(this);
        this.customer = user;
        this.products = products;
        this.quantities = quantities;
        this.discount = discount;
        this.totalPrice = totalPrice;
        this.date = date;
    }


    public Set<String> getQuantities() { return quantities; }
    public User getCustomer() { return customer; }
    public Set<Product> getProducts() { return products; }
    public Date getDate() { return date; }
    public Long getId() { return id; }
    public String getDiscount() { return discount; }
    public Integer getTotalPrice() { return totalPrice; }

    @Override
    public String toString() {
        return "Purchase{" +
                "id=" + id +
                ", totalPrice=" + totalPrice +
                ", discount=" + discount +
                '}';
    }
}
