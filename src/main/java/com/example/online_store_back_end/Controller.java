package com.example.online_store_back_end;


import jdk.nashorn.internal.ir.SplitReturn;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;


import java.util.*;
import java.util.stream.Collectors;

@RestController
public class Controller {
    private boolean isGuest(Authentication authentication) {
        return authentication == null || authentication instanceof AnonymousAuthenticationToken;
    }

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private PurchaseRepository purchaseRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    public User getAuthPerson(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName());
    }

    @RequestMapping(value= "/api/editProduct", method = RequestMethod.POST) // add a product
    public ResponseEntity<Map<String,Object>> editProduct(@RequestBody Product product, Authentication authentication) {

        User user = getAuthPerson(authentication);
        if (isGuest(authentication) || !user.getRole().contentEquals("seller")) { // Check is the current user is a seller
            return new ResponseEntity<>(makeMap("error", "You must be logged in as Seller to add a product"), HttpStatus.UNAUTHORIZED);
        }

        Product repo_product = productRepository.getOne(product.getId()); // finds the corresponding product with id

        if(!repo_product.getSeller().getId().equals(user.getId())){ // check if the product's seller ID matches this user ID
            return new ResponseEntity<>(makeMap("error", "This product does not belongs to you"), HttpStatus.UNAUTHORIZED);
        }

        return new ResponseEntity<>(makeMap("success", "Product successfully updated"), HttpStatus.ACCEPTED);

    }

    @RequestMapping(value= "/api/seller/products")
    public ResponseEntity<Map<String,Object>> getSellerProducts(Authentication authentication) {

        User user = getAuthPerson(authentication);
        if (isGuest(authentication) || !user.getRole().contentEquals("seller")) { // Check is the current user is a seller
            return new ResponseEntity<>(makeMap("error", "You must be logged in as Seller to add a product"), HttpStatus.UNAUTHORIZED);
        }
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        Set<Product> seller_products = user.getProducts(); // gets the Set of the Seller's products

        dto.put("products", seller_products.stream().map(product -> SellerProductsDTO(product)).collect(Collectors.toList()));
        return new ResponseEntity<>(dto, HttpStatus.ACCEPTED);

    }


    @RequestMapping(value = "/purchase", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> purchase(@RequestBody Set<Product> products, Authentication authentication) {

        if(isGuest(authentication)){
            return new ResponseEntity<>(makeMap("error", "You must be logged is to make a purchase"), HttpStatus.UNAUTHORIZED);
        }
        User customer = getAuthPerson(authentication);
        Date date = new Date();
        Set<Product> productSet = new HashSet<>();
        Set<String> quantities = new HashSet<>();
        String discount ="";
        Integer totalPrice = 0;

        for (Product product : products){

            Product repo_product = productRepository.getOne(product.getId());
            if ((repo_product.getStock() - product.getQuantity()) < 0) { // If the product is out of stock returns forbidden RESPONSE
                return new ResponseEntity<>(makeMap("error ", product.getName()+" is out of stock"),HttpStatus.FORBIDDEN);
            }
            if ((repo_product.getStock() - product.getQuantity()) >= 0){
                Integer freeUnits = 0;
            productSet.add(repo_product); // add the purchased products in the set
            quantities.add(repo_product.toString()+" quantity:"+product.getQuantity().toString());
            // create a "quantities" string with the matching item ant its quantity

            if(product.getQuantity()>=4){
                freeUnits = Math.round(product.getQuantity()/4); // if set the "free" value = to 1 each time there are more than 4 products
                discount = discount+ product.getName() + " : "+ freeUnits.toString()+ " free, ";
            }
            totalPrice += product.getPrice()*(product.getQuantity()-freeUnits); // total price is calculated based on each items price, quantity and free unites
            repo_product.updateStock(product.getQuantity()); // update the product's stock (substract the quantity)
            }
        }

        List<Map<String,String>> items = new ArrayList<>();
        quantities.forEach(item->items.add(convert(item)));// convert the quanties string to a List of maps
        Purchase newPurchase = new Purchase(productSet,quantities,discount,totalPrice,customer,date);
        purchaseRepository.save(newPurchase);

        return new ResponseEntity<>(makeMap("success","Purchase realized"),HttpStatus.CREATED);
    }


    @RequestMapping("/api/customers") // Return an API with all the books
    public Map<String, Object>  getCustomers() {
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        Set<User> customers = new HashSet<>();
        userRepository.findAll().stream().map(user ->{if (user.getRole().contentEquals("customer")){customers.add(user);}return customers;}).collect(Collectors.toList());
// check in the userRepository which Person as the role of "customer" and add these persons to the Set of Customers
        dto.put("customers", customers.stream().map(customer->CustomerDTO(customer)).collect(Collectors.toList()));
        return dto;
    }
    @RequestMapping("/api/sellers") // Return an API with all the books
    public Map<String, Object>  getAuthors() {
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        Set<User> sellers = new HashSet<>();
        userRepository.findAll().stream().map(person ->{if (person.getRole().contentEquals("seller")){sellers.add(person);}return sellers;}).collect(Collectors.toList());
        // check in the userRepository which Person as the role of "seller" and add these persons to the Set of Sellers
        dto.put("seller", sellers.stream().map(seller->SellerDTO(seller)).collect(Collectors.toList()));
        return dto;
    }

    @RequestMapping("/api/products")
    public Map<String, Object>  getProducts(Authentication authentication) {
    Map<String,Object> dto = new LinkedHashMap<>();
    if(isGuest(authentication)){
         dto.put("user", null);
    } if (!isGuest(authentication)){
        dto.put("user", loginDTO(authentication));
        }
        dto.put("products", productRepository.findAll().stream().map(product -> productDTO(product)).collect(Collectors.toList()));
    return dto;

    }
    private Map<String, Object> SellerDTO(User seller) {  // makes the Book DTO
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("firstName", seller.getFirstName());
        dto.put("lastName", seller.getLastName());
        dto.put("email", seller.getEmail());
        dto.put("products", seller.getProducts().stream().map(product -> SellerProductsDTO(product)));

        return dto;
    }
    private Map<String, Object> SellerProductsDTO(Product product) {
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("id", product.getId());
        dto.put("name", product.getName());
        dto.put("category", product.getCategory());
        dto.put("price", product.getPrice());
        dto.put("description", product.getDescription());
        dto.put("stock", product.getStock());
        dto.put("quantity", product.getQuantity());
        dto.put("images",product.getImages());

        return dto;
    }
    private Map<String, Object> CustomerDTO(User customer) {
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("firstName", customer.getFirstName());
        dto.put("lastName", customer.getLastName());
        dto.put("email", customer.getEmail());
        dto.put("purchases", customer.getPurchases().stream().map(purchase ->PurchaseDTO(purchase)));
        return dto;
    }
    private  Map<String, Object> PurchaseDTO(Purchase purchase){
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("id", purchase.getId());
        dto.put("date", purchase.getDate());
        dto.put("discount",purchase.getDiscount());
        dto.put("price", purchase.getTotalPrice());
//        dto.put("products", purchase.getProducts().stream().map(product -> PurchasedProductsDTO(product)));
//        dto.put("purchases", purchase.getQuantities().stream().map(qty->QuantityDTO(qty)));
        dto.put("purchases", purchase.getQuantities().stream().map(qty->convert(qty)));
        return dto;
    }
    private Map<String, Object> QuantityDTO(String string) {
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("id", string);
        return  dto;
    }
    private Map<String, Object> PurchasedProductsDTO(Product product) {
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("id", product.getId());
        dto.put("title", product.getName());
        dto.put("quantity", product.getQuantity());
        return  dto;
    }
    private Map<String,Object> productDTO(Product product){
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("id", product.getId());
        dto.put("name", product.getName());
        dto.put("category", product.getCategory());
        dto.put("price", product.getPrice());
        dto.put("description", product.getDescription());
        dto.put("stock", product.getStock());
        dto.put("quantity", product.getQuantity());
        dto.put("images",product.getImages());
        dto.put("rate",product.getRate());
        dto.put("seller", ProductSellerDTO(product.getSeller()));

         if(product.getPurchases().size()==0){
            dto.put("purchases", "none");
        } if(product.getPurchases().size()>0){
            dto.put("purchases", product.getPurchases().stream().map(purchase -> PurchaseDTOforProducts(purchase, product)));
        }

        return dto;
    };
    private Map<String, Object> CustomerPurchaseDTO(User customer) {
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("firstName", customer.getFirstName());
        dto.put("lastName", customer.getLastName());
        dto.put("email", customer.getEmail());
        return dto;
    }
    private  Map<String, Object> PurchaseDTOforProducts(Purchase purchase,Product product){
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("id", purchase.getId());
        dto.put("date", purchase.getDate());
        dto.put("quantity", purchase.getQuantities().stream().map(qty->getQty(qty,product)).map(ty-> {if(ty>0){return ty;}return "";}));
        dto.put("customer",CustomerPurchaseDTO(purchase.getCustomer()));
        return dto;
    }
    public static Map<String, String> convert(String str) { // convert a given string into a map (used to Obtain the product's pruchase details)
        String[] tokens = str.split(" |:");
        Map<String, String> map = new HashMap<>();
        for (int i=0; i<tokens.length-1; ) map.put(tokens[i++], tokens[i++]);
//        System.out.println(map);
        return map;
    }

    public static int getQty(String str, Product product) { // Specifically return the quantity of the matching product
        String[] tokens = str.split(" |:");
        Map<String, String> map = new HashMap<>();
        String quantity = "0";
        for (int i=0; i<tokens.length-1; ) {
            map.put(tokens[i++], tokens[i++]);
        }
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if(key.contentEquals("Product_id")&& value.toString().contentEquals(product.getId().toString())){
                quantity = map.get("quantity");
            }
        }
        return Integer.parseInt(quantity);
    }


    private Map<String, Object> ProductSellerDTO(User user) {  // makes the Book DTO
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("id",user.getId());
        dto.put("firstName", user.getFirstName());
        dto.put("lastName", user.getLastName());

        return dto;
    }
    private Map<String, Object> loginDTO(Authentication authentication) { // Loging DTO will check which user is logged in and will return the apropriate information
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        User user = getAuthPerson(authentication);
        if(user != null) {
            dto.put("firstName", userRepository.findByEmail(authentication.getName()).getFirstName());
            dto.put("lastName", userRepository.findByEmail(authentication.getName()).getLastName());
            dto.put("email", userRepository.findByEmail(authentication.getName()).getEmail());
            dto.put("role",userRepository.findByEmail(authentication.getName()).getRole());
        }
        return dto;
    }

    @RequestMapping(value= "/api/addProduct", method = RequestMethod.POST) // add a product
    public ResponseEntity<Map<String,Object>> addProduct(@RequestBody Product product, Authentication authentication) {

        User user = getAuthPerson(authentication);
//        System.out.println(user.getRole());
        if (isGuest(authentication) || !user.getRole().contentEquals("seller")) { // Check is the current user is a seller
            return new ResponseEntity<>(makeMap("error", "You must be logged in as Seller to add a product"), HttpStatus.UNAUTHORIZED);
        }

        Product isDuplicate = productRepository.findByName(product.getName());
        if (isDuplicate != null) {
            return new ResponseEntity<>(makeMap("error", "This product already exists"), HttpStatus.CONFLICT);
        }
        Product newProduct = new Product(product.getName(),product.getPrice(), product.getDescription(),product.getCategory(),product.getStock(), user);
        user.addProduct(newProduct);
        productRepository.save(newProduct);
        return new ResponseEntity<>(makeMap("success", "New product added"), HttpStatus.ACCEPTED);

    }


    @RequestMapping(value = "/api/signup", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> addPerson(@RequestBody User user) { // Add a Perosn
        User isPerson = userRepository.findByEmail(user.getEmail());

        if (isPerson != null) {
            return new ResponseEntity<>(makeMap("error","Person already exists"),HttpStatus.CONFLICT);
        }
        User newUser = new User(user.getFirstName(),user.getLastName(),user.getEmail(),user.getRole(),passwordEncoder.encode(user.getPassword()));
        userRepository.save(newUser);
        return new ResponseEntity<>(makeMap("success","Person Added"),HttpStatus.CREATED);
    }
    private Map<String, Object> makeMap(String key, Object value) { // Makes the response sent with the response entity
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return map;
    }




}
