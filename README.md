# 3DForgeMarket

3DForgeMarket is a web application for browsing and ordering 3D-printed products. It is designed as a small online 3D-print shop where customers can explore available products, place orders, track their own orders, and leave reviews after successful delivery.

Administrators manage the product catalogue, review all customer orders, update printing and delivery statuses, and moderate product reviews.

> Project status: under development.

## Technology Stack

* Java 17
* Spring Boot 3.4.0
* Maven
* Spring MVC
* Thymeleaf
* Spring Data JPA
* Spring Validation
* MySQL
* Lombok
* HTML, CSS, and minimal vanilla JavaScript
* Git and GitHub

## Main Entities

### User

Represents a registered customer or administrator.

Main data:

* Username
* Email
* Hashed password
* Role
* Account creation date
* Last login date

A user can have multiple customer orders.

### Product

Represents a 3D-printed product available in the catalogue.

Main data:

* Name
* Description
* Price in EUR
* Product image URL
* Optional printable-model URL
* Estimated print time
* Width, height, and depth in centimeters
* Estimated weight in grams
* Product category
* Print material
* Colour description
* Availability status
* Creation and update dates

### CustomerOrder

Represents an order made by a customer for one product.

Main data:

* Ordered product
* Customer
* Quantity
* Delivery address
* Optional customer note
* Total price in EUR
* Order status
* Creation and update dates

### Review

Represents customer feedback for a product.

Main data:

* Product
* Author
* Rating
* Comment
* Creation and update dates

A customer can leave only one review for the same product.

## User Roles and Permissions

### Guest

Guests can:

* View the home page
* Browse the product catalogue
* View product details
* Register
* Log in

### Customer

Logged-in customers can:

* Place orders for available products
* View only their own orders
* Cancel eligible orders
* View product reviews
* Create, edit, and delete their own reviews when allowed

### Administrator

Administrators can:

* Create products
* Edit products
* Change product availability
* Delete products
* View every customer order
* Update order statuses
* View and moderate reviews

## Planned Functionalities

### Product Catalogue Management

Administrators can create, edit, hide, show, and delete products.

Each product includes 3D-print-specific information such as material, dimensions, estimated weight, print duration, colour description, image URL, and optional model URL.

### Product Browsing

Guests and logged-in users can browse available products, search by name, and filter products by category.

### Customer Orders

Customers can place an order for an available product by selecting a quantity and providing a delivery address.

The total price is calculated on the server based on the product price and selected quantity.

### Customer Order History

Customers can view only their own orders and cancel orders only when the current status allows cancellation.

### Admin Order Management

Administrators can view all customer orders and update their status through the printing and delivery workflow:

* Pending
* Confirmed
* Printing
* Ready for Delivery
* Delivered
* Cancelled

### Product Reviews

Customers can leave a review for a delivered product.

Customers can edit or delete only their own reviews. Administrators can remove inappropriate reviews.

## Planned Pages

* `/` — Home page
* `/auth/login` — Login page
* `/auth/register` — Registration page
* `/products` — Product catalogue
* `/products/{id}` — Product details
* `/orders/create` — Create order page
* `/orders/my` — Current customer order history
* `/reviews/{id}/edit` — Edit review page
* `/admin/products` — Product management page
* `/admin/products/new` — Create product page
* `/admin/products/{id}/edit` — Edit product page
* `/admin/orders` — Admin order-management page
* `/admin/reviews` — Review-moderation page

## Security Approach

The application uses session-based authentication.

After a successful login, the authenticated user's identifier is stored in the HTTP session. Access to customer and administrator pages is controlled according to the current session user and their assigned role.

Passwords are stored only as hashed values. Plain-text passwords must never be stored, logged, or committed to the repository.

## Database Configuration

The application uses MySQL with Spring Data JPA.

Database connection values are configured through environment variables. At minimum, configure:

```text
DB_USERNAME
DB_PASSWORD
```

The local database configuration is defined in:

```text
src/main/resources/application.properties
```

Do not commit database passwords, API keys, access tokens, or other sensitive values to the public repository.

## Running the Application Locally

1. Install Java 17.
2. Install and start MySQL.
3. Create or configure the local project database.
4. Set the required environment variables:

```text
DB_USERNAME=your_mysql_username
DB_PASSWORD=your_mysql_password
```

5. Start the application with Maven:

```bash
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

6. Open the application in a browser:

```text
http://localhost:8080
```

## External Image and Model Storage

Product images and public GLB preview models are stored in a public Supabase Storage bucket.

The bucket is organized into two folders:

- `images/` — product images
- `models/` — public GLB product preview models

The database stores the public URLs for these files in `imageUrl` and `modelUrl`.

Because the bucket is public, anyone with an asset URL can access it. Only public preview assets should be uploaded there.

## Notes

This project is an original Spring Fundamentals course project. The application is being developed incrementally with focus on clear MVC structure, validation, role-based access control, simple entity relationships, and maintainable code.
