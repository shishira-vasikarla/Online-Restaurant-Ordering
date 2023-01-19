import java.sql.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicReference;

public class Restaurant {

    Connection connection;
    Statement statement;

    Restaurant(String db_name, String db_username, String db_password) throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/", db_username,
                db_password); // username and passcode for entering sql.

        Statement statement = connection.createStatement();

        String query = "CREATE DATABASE IF NOT EXISTS " + db_name;
        statement.executeUpdate(query);
        connection.close();

        this.connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/" + db_name, db_username,
                db_password);
        this.statement = this.connection.createStatement();

        String create_customer_table = "create table if not exists customer (username varchar(20) primary key, " +
                "password varchar(20), name varchar(25), phone_num varchar(13), email varchar(25), " +
                "address varchar(150), zipcode varchar(10));";
        this.statement.executeUpdate(create_customer_table);

        String create_admin_table = "create table if not exists admin (username varchar(25) primary key, " +
                "password varchar(25));";
        this.statement.executeUpdate(create_admin_table);

        String create_item_table = "create table if not exists item (item_id int not null auto_increment, " +
                "name varchar(25), category varchar(15), price double, primary key (item_id));";
        this.statement.executeUpdate(create_item_table);

        String create_order_table = "create table if not exists orders (order_id int not null, " +
                "username varchar(20) not null, item_id int not null, quantity int, primary key (order_id, item_id));";
        this.statement.executeUpdate(create_order_table);

        String create_order_summary_table = "create table if not exists order_summary (" +
                "order_id int not null primary key, username varchar(20) not null, total_bill double, order_at timestamp);";
        this.statement.executeUpdate(create_order_summary_table);
    }

    public void showMenu() throws SQLException {
        String view = "select * from item;";
        ResultSet resultSet = statement.executeQuery(view);
        System.out.println();
        while (resultSet.next()){
            System.out.println("----------------------------------------------------------------");
            System.out.printf("""
                    Item ID: %d
                    Name: %s
                    Category: %s
                    Price: %.2f
                    """, resultSet.getInt(1), resultSet.getString(2),
                    resultSet.getString(3), resultSet.getDouble(4));
            System.out.println("----------------------------------------------------------------");
            System.out.println();
        }
    }

    public void addItem(String name, String category, double price) throws SQLException {
        String insert = String.format("insert into item (name, category, price) values ('%s', '%s', '%.2f');", name,
                category, price);
        statement.executeUpdate(insert);
    }

    public void modifyItem(int item_id, String field, String value) throws SQLException {
        String update = String.format("update item set %s = '%s' where item_id = '%s';", field, value, item_id);
        statement.executeUpdate(update);
    }

    public void modifyItem(int item_id, String field, double value) throws SQLException {
        String update = String.format("update item set %s = '%s' where item_id = %d;", field, value, item_id);
        statement.executeUpdate(update);
    }

    public void deleteItem(int item_id) throws SQLException {
        String delete = "delete from item where item_id = " + item_id;
        statement.executeUpdate(delete);
    }

    public ResultSet getItem(int item_id) throws SQLException {
        String query = "select * from item where item_id = " + item_id;
        return statement.executeQuery(query);
    }

    public void placeOrder(String username, ArrayList<Item> items, double total) throws SQLException {
        String query = "SELECT MAX(order_id) FROM order_summary;";
        ResultSet resultSet = statement.executeQuery(query);
        int order_id;
        if(resultSet.next()){
            order_id = resultSet.getInt(1) + 1;
        }
        else {
            order_id = 1;
        }

        items.forEach((item) -> {
            String insert = String.format("insert into orders (order_id, username, item_id, quantity) " +
                    "values (%d, '%s', %d, %d);", order_id, username, item.id, item.quantity);
            try {
                statement.executeUpdate(insert);
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        });

        String insert = String.format("insert into order_summary (order_id, username, total_bill, order_at) " +
                "values (%d, '%s', %.2f, CURRENT_TIME());", order_id, username, total);
        statement.executeUpdate(insert);
        System.out.println("Order placed successfully!");
    }

    public boolean login(String username, String password, boolean isAdmin) throws SQLException {
        String query;

        if(isAdmin) {
             query = String.format("select password from admin where username = '%s';", username);
        } else {
            query = String.format("select password from customer where username = '%s';", username);
        }

        ResultSet resultSet = statement.executeQuery(query);
        if (resultSet.next()) {
            return resultSet.getString(1).equals(password);
        }
        return false;
    }

    public void registerCustomer(String username, String password, String name, String phone_num, String email, String address, String zipcode) throws SQLException {
        String insert = String.format("insert into customer (username, password, name, phone_num, email, address, zipcode) " +
                "values ('%s', '%s', '%s', '%s', '%s', '%s', '%s');", username, password, name, phone_num, email, address, zipcode);
        statement.executeUpdate(insert);
    }

    public void viewCustomer(String username) throws SQLException {
        String query = String.format("select * from customer where username = '%s';", username);
        ResultSet resultSet = statement.executeQuery(query);
        System.out.println();
        if (resultSet.next()){
            System.out.printf("""
                    Username: %s
                    Name: %s
                    Phone number: %s
                    Email: %s
                    Address: %s
                    Zipcode: %s
                    """, resultSet.getString(1), resultSet.getString(3),
                    resultSet.getString(4), resultSet.getString(5),
                    resultSet.getString(6), resultSet.getString(7));
        }
        else {
            System.out.printf("Customer with username '%s' does not exist", username);
        }

    }

    public void viewAllCustomers() throws SQLException {
        String query = "select * from customer;";
        ResultSet resultSet = statement.executeQuery(query);
        System.out.println();
        while (resultSet.next()){
            System.out.println("----------------------------------------------------------------");
            System.out.printf("""
                    Username: %s
                    Name: %s
                    Phone number: %s
                    Email: %s
                    Address: %s
                    Zipcode: %s
                    """, resultSet.getString(1), resultSet.getString(3),
                    resultSet.getString(4), resultSet.getString(5),
                    resultSet.getString(6), resultSet.getString(7));
            System.out.println("----------------------------------------------------------------");
            System.out.println();
        }
    }

    public void viewOrdersByCustomer(String username) throws SQLException {
        String query = String.format("select * from order_summary where username = '%s' order by order_at desc;", username);
        ResultSet resultSet = statement.executeQuery(query);
        System.out.println();
        while (resultSet.next()){
            int order_id = resultSet.getInt("order_id");
            double total = resultSet.getDouble("total_bill");
            String time = resultSet.getString("order_at");

            System.out.println();
            System.out.println("----------------------------------------------------------------");
            System.out.println("Order ID: " + order_id);
            System.out.println("Ordered at: " + time);
            System.out.println();

            query = String.format("select orders.item_id, item.name, item.price, orders.quantity from item " +
                    "inner join orders on item.item_id = orders.item_id where orders.order_id = %d;", order_id);

            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(query);
            while (rs.next()){
                System.out.printf("""
                        Item ID: %d
                        Item Name: %s
                        Item Price: %.2f
                        Item Quantity: %d
                        """, rs.getInt("item_id"), rs.getString("name"),
                        rs.getDouble("price"), rs.getInt("quantity"));
                System.out.println();
            }
            System.out.println("Total Bill: " + total);
            System.out.println("----------------------------------------------------------------");
            st.close();

        }
    }

    public void viewOrders() throws SQLException {
        String query = "select * from order_summary order by order_at desc;";
        ResultSet resultSet = statement.executeQuery(query);
        System.out.println();
        while (resultSet.next()){
            int order_id = resultSet.getInt("order_id");
            String username = resultSet.getString("username");
            double total = resultSet.getDouble("total_bill");
            String time = resultSet.getString("order_at");

            System.out.println("----------------------------------------------------------------");
            System.out.println("Order ID: " + order_id);
            System.out.println("Ordered by: " + username);
            System.out.println("Ordered at: " + time);
            System.out.println();

            query = String.format("select orders.item_id, item.name, item.price, orders.quantity from item " +
                    "inner join orders on item.item_id = orders.order_id where orders.order_id = %d;", order_id);

            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(query);
            while (rs.next()){
                System.out.printf("""
                        Item ID: %d
                        Item Name: %s
                        Item Price: %.2f
                        Item Quantity: %d
                        """, rs.getInt("item_id"), rs.getString("name"),
                        rs.getDouble("price"), rs.getInt("quantity"));
                System.out.println();
            }
            st.close();
            System.out.println("Total Bill: " + total);
            System.out.println("----------------------------------------------------------------");
        }
    }

    public void modifyCustomers(String username, String field, String value) throws SQLException {
        String update = String.format("update customer set %s = '%s' where username = '%s';", field, value, username);
        statement.executeUpdate(update);
    }

    public void deleteCustomer(String username) throws SQLException {
        String delete = String.format("delete from item where username = '%s'", username);
        statement.executeUpdate(delete);
    }

    public static void main(String[] args) throws SQLException, ClassNotFoundException {

        Class.forName("com.mysql.cj.jdbc.Driver");
        String db_name = "restaurant";
        String db_username = "root";
        String db_password = "Mysql123";

        Restaurant restaurant = new Restaurant(db_name, db_username, db_password);
        Scanner s = new Scanner(System.in);
        System.out.println("Hello! Welcome to our Restaurant");

        while (true) {
            System.out.println("\t1. View menu");
            System.out.println("\t2. Login as Customer");
            System.out.println("\t3. Register as Customer");
            System.out.println("\t4. Login as Admin");
            System.out.println("\tq. Quit");

            String choice = s.nextLine();

            if (choice.toLowerCase().equals("q")) {
                System.out.println("Bye!");
                break;
            }

            try {
                int c = Integer.parseInt(choice);
                switch (c) {
                    case 1 -> restaurant.showMenu();
                    case 2 -> {
                        System.out.println("Enter username: ");
                        String username = s.nextLine();
                        System.out.println("Enter password: ");
                        String password = s.nextLine();
                        if (restaurant.login(username, password, false)) {
                            while (true) {
                                System.out.println("Welcome " + username);
                                System.out.println("\t1. View Menu");
                                System.out.println("\t2. Place Order");
                                System.out.println("\t3. Update Profile");
                                System.out.println("\t4. View Orders");
                                System.out.println("\tq. Logout");

                                choice = s.nextLine();

                                if (choice.toLowerCase().equals("q")) {
                                    System.out.println("Logged out!");
                                    break;
                                }

                                try {
                                    c = Integer.parseInt(choice);
                                    switch (c) {
                                        case 1 -> restaurant.showMenu();
                                        case 2 -> {
                                            ArrayList<Item> items = new ArrayList<>();
                                            AtomicReference<Double> total = new AtomicReference<>((double) 0);
                                            while (true){
                                                restaurant.showMenu();
                                                System.out.println("Enter item id");
                                                c = s.nextInt(); s.nextLine();
                                                System.out.println("Enter quantity");
                                                int q = s.nextInt(); s.nextLine();
                                                ResultSet rs = restaurant.getItem(c);
                                                if(rs.next()) {
                                                    double price = rs.getDouble("price");
                                                    String name = rs.getString("name");
                                                    if (price == -1){
                                                        System.out.println("Invalid item id");
                                                        continue;
                                                    }

                                                    items.add(new Item(c, name, price, q));
                                                    System.out.println("\nDo you want to add more items? (y/n)");
                                                    choice = s.nextLine();
                                                    if(!choice.toLowerCase().equals("y")){
                                                        break;
                                                    }
                                                }
                                                else {
                                                    System.out.println("Invalid item id");
                                                }
                                            }

                                            System.out.println("Selected items");
                                            items.forEach((item) -> {
                                                total.updateAndGet(v -> v + item.quantity * item.price);
                                                System.out.printf("""
                                                        Item ID %d
                                                        Item Name %s
                                                        Quantity: %d
                                                        Price: %.2f
                                                        """, item.id, item.name, item.quantity, item.price);
                                            });
                                            System.out.println();
                                            System.out.println("Total bill: " +  total);

                                            System.out.println("\nDo you want to place order? (y/n)");
                                            choice = s.nextLine();
                                            if(choice.toLowerCase().equals("y")){
                                                restaurant.placeOrder(username, items, total.get());
                                            }
                                            else {
                                                System.out.println("Order discarded");
                                            }
                                        }
                                        case 3 -> {
                                            restaurant.viewCustomer(username);

                                            System.out.println("Which field do you want to change?");
                                            System.out.println("\t1. Name");
                                            System.out.println("\t2. Phone");
                                            System.out.println("\t3. Email");
                                            System.out.println("\t4. Address");
                                            System.out.println("\t5. Zipcode");
                                            System.out.println("Enter your choice (q to cancel)");

                                            choice = s.nextLine();

                                            if (choice.toLowerCase().equals("q")) {
                                                System.out.println("Bye!");
                                                break;
                                            }

                                            try {
                                                c = Integer.parseInt(choice);
                                                String field = "";
                                                boolean correctInput = true;
                                                switch (c){
                                                    case 1 -> field = "name";
                                                    case 2 -> field = "phone_num";
                                                    case 3 -> field = "email";
                                                    case 4 -> field = "address";
                                                    case 5 -> field = "zipcode";
                                                    default -> {
                                                        System.out.println("Invalid choice");
                                                        correctInput = false;
                                                    }
                                                }

                                                if(correctInput) {
                                                    System.out.println("Enter new value");
                                                    choice = s.nextLine();
                                                    restaurant.modifyCustomers(username, field, choice);
                                                    System.out.println("Details have been updated!");
                                                }

                                            }
                                            catch (Exception e) {
                                                System.out.println("Invalid choice");
                                            }
                                        }
                                        case 4 -> {
                                            restaurant.viewOrdersByCustomer(username);
                                        }
                                    }
                                }  catch (Exception e) {
                                    System.out.println(e.toString());
                                }
                            }
                        } else {
                            System.out.println("Invalid Credentials!");
                        }
                    }
                    case 3 -> {
                        System.out.println("Enter username: ");
                        String username = s.nextLine();
                        System.out.println("Enter password: ");
                        String password = s.nextLine();
                        System.out.println("Enter name: ");
                        String name = s.nextLine();
                        System.out.println("Enter phone number: ");
                        String phone_num = s.nextLine();
                        System.out.println("Enter email: ");
                        String email = s.nextLine();
                        System.out.println("Enter address: ");
                        String address = s.nextLine();
                        System.out.println("Enter zipcode: ");
                        String zipcode = s.nextLine();
                        restaurant.registerCustomer(username, password, name, phone_num, email, address, zipcode);

                    }
                    case 4 -> {
                        System.out.println("Enter username: ");
                        String username = s.nextLine();
                        System.out.println("Enter password: ");
                        String password = s.nextLine();
                        if (restaurant.login(username, password, true)) {
                            while (true) {
                                System.out.println("\t1. View All Items");
                                System.out.println("\t2. Add an Item");
                                System.out.println("\t3. Modify an Item");
                                System.out.println("\t4. Delete an Item");
                                System.out.println("\t5. View all Customers");
                                System.out.println("\t6. View all Orders");
                                System.out.println("\t7. View Orders by Customer");
                                System.out.println("\tq. Quit");

                                choice = s.nextLine();

                                if (choice.toLowerCase().equals("q")) {
                                    System.out.println("Bye!");
                                    break;
                                }

                                try {
                                    c = Integer.parseInt(choice);
                                    switch (c) {
                                        case 1 -> restaurant.showMenu();
                                        case 2 -> {
                                            System.out.println("Enter name");
                                            String name = s.nextLine();
                                            System.out.println("Enter category");
                                            String category = s.nextLine();
                                            System.out.println("Enter price");
                                            double price = s.nextDouble();
                                            s.nextLine();
                                            restaurant.addItem(name, category, price);
                                        }
                                        case 3 -> {
                                            restaurant.showMenu();
                                            System.out.println("Select item by ID");
                                            int id = s.nextInt();
                                            System.out.println("Select field");
                                            System.out.println("1. Name");
                                            System.out.println("2. Category");
                                            System.out.println("3. Price");
                                            c = s.nextInt();
                                            s.nextLine();
                                            String field = "";

                                            switch (c) {
                                                case 1 -> field = "name";
                                                case 2 -> field = "category";
                                                case 3 -> field = "price";
                                            }

                                            System.out.println("Enter new value");

                                            String value = s.nextLine();

                                            if (c == 4) {
                                                double price = Double.parseDouble(value);
                                                restaurant.modifyItem(id, field, price);
                                            } else {
                                                restaurant.modifyItem(id, field, value);
                                            }
                                            System.out.println("Item updated successfully!");
                                        }

                                        case 4 -> {
                                            restaurant.showMenu();
                                            System.out.println("Select item by ID");
                                            int id = s.nextInt(); s.nextLine();
                                            restaurant.deleteItem(id);
                                            System.out.println("Item deleted successfully!");
                                        }

                                        case 5 -> {
                                            restaurant.viewAllCustomers();
                                        }

                                        case 6 -> {
                                            restaurant.viewOrders();
                                        }

                                        case 7 -> {
                                            System.out.println("Enter username of customer");
                                            username = s.nextLine();
                                            restaurant.viewOrdersByCustomer(username);
                                        }
                                    }
                                } catch (Exception e) {
                                    System.out.println(e.toString());
                                }

                            }
                        }

                        else{
                                System.out.println("Invalid Credentials!");
                            }
                    }
                }
            } catch (Exception e) {
                System.out.println(e.toString());
            }

        }
    }
}

class Item {
    int id;
    String name;
    double price;
    int quantity;

    Item(int id, String name, double price, int quantity){
        this.id = id;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
    }
}
