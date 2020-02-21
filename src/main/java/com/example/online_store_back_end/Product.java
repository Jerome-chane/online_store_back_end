package com.example.online_store_back_end;
import org.hibernate.annotations.GenericGenerator;
import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;


@Entity
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private Long id;
    private String name;
    private Integer price;
    private String description;
    private String category;
    private Integer stock;
    private Integer quantity =1;
    private Integer rate = null;

    @ElementCollection
    @OrderColumn(name="images_id")
    private Set<String> images = new HashSet<>();


    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name="seller_id")
    private User seller;

    @ManyToMany(mappedBy = "products")
    Set<Purchase> purchases;

    public Product() { };

    public Product(String name, Integer price, String description, String category, Integer stock, User seller) {
        this.name = name;
        this.price = price;
        this.description = description;
        this.category = category;
        this.stock = stock;
        this.seller = seller;
        seller.addProduct(this);
    }

    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Set<Purchase> getPurchases() { return purchases; }
    public void addPurchase(Purchase purchase){ this.purchases.add(purchase);}
    public User getSeller() { return seller;}
    public void setRate(Integer rate) { this.rate = rate; }
    public void addImage(String link){ this.images.add(link); };
    public Long getId() { return id; }
    public String getDescription() { return description; }
    public String getName() { return name; }
    public Integer getPrice() { return price; }
    public Integer getStock() { return stock; }
    public Set<String> getImages() { return images; }
    public String getCategory() { return category; }
    public Integer getRate() { return rate; }
    public Integer getQuantity() { return quantity; }

    @Override
    public String toString() {
        return "Product_id:" + id +
                " name:" + name  +
                " price:" + price ;
    }
}
