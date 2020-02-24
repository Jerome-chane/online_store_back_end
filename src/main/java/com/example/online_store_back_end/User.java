package com.example.online_store_back_end;

import org.hibernate.annotations.GenericGenerator;
import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;


@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String role;

    @OneToMany(mappedBy="seller", fetch=FetchType.EAGER)
    Set<Product> products = new HashSet<>();

    @OneToMany(mappedBy="customer", fetch=FetchType.EAGER)
    Set<Purchase> purchases = new HashSet<>();

    public User() { }

    public User(String firstName, String lastName ,String email, String role, String password) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.role = role;

    }

    public Long getId() {
        return id;
    }
    public String getRole() { return role; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public Set<Product> getProducts() { return products; }
    public Set<Purchase> getPurchases() { return purchases; }

    public void addPurchase(Purchase purchase){this.purchases.add(purchase);}
    public void addProduct(Product product) {
        if (this.role.contentEquals("seller")) {
            this.products.add(product);
        }
        if (!this.role.contentEquals("seller")) {
            System.out.println("unable to add product because USER is not a sellet");
        }
    }


//    public void addPurchase(Purchase purchase){this.purchases.add(purchase);}

    public void setEmail(String email) {this.email = email; }
    public void setPassword(String password) { this.password = password; }


    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + firstName + '\'' +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}