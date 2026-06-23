# 3DForgeMarket

3DForgeMarket is a Spring Boot web application for browsing and ordering 3D-printed products.

Guests can explore the catalogue and product details. Customers can place orders, manage their own order history, and review products after successful delivery. Administrators manage products, customer orders, order-status progression, and review moderation.

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
* BCrypt password hashing
* HTML, CSS, and minimal vanilla JavaScript
* Local GLB preview support through `model-viewer`
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

### Product

Represents a 3D-printed product in the catalogue.

Main data:

* Name and description
* Price in EUR
* Product image URL
* Optional public GLB preview URL
* Estimated print time
* Width, height, and depth in centimeters
* Estimated weight in grams
* Product category
* Print material
* Colour description
* Availability status
* Creation and update dates

### CustomerOrder

Represents an order created by a customer for one product.

Main data:

* Ordered product
* Customer
* Quantity
* Delivery address
* Optional customer note
* Server-calculated total price in EUR
* Order status
* Customer/admin history visibility settings
* Creation and update dates

### Review

Represents customer feedback for a delivered product.

Main data:

* Product
* Author
* Rating from 1 to 5
* Comment
* Creation and update dates

A customer can submit only one review for the same product.

## User Roles and Permissions

### Guest

Guests can:

* View the home page
* Browse the available product catalogue
* Search and filter products
* View product details and public reviews
* Register
* Log in

### Customer

Customers can:

* Place orders for available products
* View only their own order history
* Cancel eligible orders
* Remove eligible completed or cancelled orders from their own history
* Submit one review per product after at least one delivered order
* Edit or delete only their own reviews
* View and update their own profile information

### Administrator

Administrators can:

* Create, edit, hide, show, and delete eligible products
* Search and filter all products, including hidden products
* View all customer orders
* Update order statuses through the allowed workflow
* Remove eligible completed or cancelled orders from the admin history
* View and delete customer reviews
* View and update their own profile information

## Implemented Functionalities

### Home Page and Product Discovery

The home page presents the marketplace identity and featured available products.

#### Home Page

The home page introduces 3DForgeMarket and highlights recently available products for guests and authenticated users.

<img width="1919" height="920" alt="3DForgeMarket home page with featured products" src="https://github.com/user-attachments/assets/edbcc2d5-5f80-4f58-9877-2181d490f1d0" />

#### Product Catalogue

Guests and authenticated users can browse the public catalogue, search products by name, and filter products by category.

<img width="1919" height="919" alt="Product catalogue with search and category filtering" src="https://github.com/user-attachments/assets/bb5390c2-1ba5-4a64-83fd-0e2645d33c4e" />

### Product Details and 3D Preview

Each product details page displays product information such as:

* Description
* Price
* Dimensions
* Weight
* Material
* Colour description
* Estimated print time
* Availability
* Product image
* Optional public GLB 3D preview

Public reviews are shown on the product-details page and are ordered by highest rating first. Reviews with equal ratings are ordered newest first.

#### Product Details and GLB Preview

<img width="1628" height="840" alt="Product details page with product information and GLB 3D preview" src="https://github.com/user-attachments/assets/17c7b54d-491d-4c8d-8280-1fb5789ef344" />

#### Customer Reviews on Product Details

<img width="1361" height="780" alt="Product details page showing customer reviews and star ratings" src="https://github.com/user-attachments/assets/0fde946e-e094-4480-85da-3ae32d91ce61" />

### Product Management

Administrators can manage products through full CRUD functionality:

* Create products
* Edit products
* Hide or show products
* Search and filter products
* Delete products that have no order history

Products with existing orders cannot be deleted. They must be hidden instead, preserving historical order data.

#### Admin Product Management

<img width="1919" height="791" alt="Admin product management page with filters and product actions" src="https://github.com/user-attachments/assets/c8ef2f5d-97ae-43b0-8d59-c0d0ca042dc5" />

### Customer Orders

Customers can place orders for available products by selecting a quantity, entering a delivery address, and optionally adding a customer note.

The total price is calculated on the server using the current product price and selected quantity.

Customers can cancel orders only while the status is:

* Pending
* Confirmed

Customers can remove eligible delivered or cancelled orders from their own order-history view.

#### Customer Order History

<img width="1339" height="311" alt="Customer order history with order statuses and actions" src="https://github.com/user-attachments/assets/27ce4768-78a2-4b57-9269-bdc4f55e4fb0" />

### Admin Order Management

Administrators can view all customer orders and update their statuses through the allowed workflow:

* Pending → Confirmed or Cancelled
* Confirmed → Printing or Cancelled
* Printing → Ready for Delivery
* Ready for Delivery → Delivered
* Delivered and Cancelled orders cannot be updated further

#### Admin Order Management

<img width="1254" height="574" alt="Admin order management page with status update controls" src="https://github.com/user-attachments/assets/17298e20-7419-4afe-b1d0-aec432bd3a83" />

### Product Reviews

Customers can review a product only after at least one delivered order for that product.

Review functionality includes:

* Rating validation from 1 to 5
* Comment-length validation
* One review per customer per product
* Customer edit and delete actions for their own reviews
* Administrator review moderation through deletion
* Star-based rating presentation

#### Admin Review Moderation

<img width="1306" height="364" alt="Admin review moderation page with product reviews and delete actions" src="https://github.com/user-attachments/assets/05144d63-e805-4204-b01f-61dde6b8af25" />

### Profile Management

Every authenticated user can access a personal profile page from the username dropdown in the navigation menu.

The profile page displays:

* Username
* Email
* Role
* Account creation date
* Last login date

Users can update only their own username and email address.

Profile updates include server-side validation and uniqueness checks for both username and email. Passwords, roles, internal UUIDs, account creation dates, and login history cannot be edited through the profile form.

#### My Profile

<img width="1072" height="618" alt="Authenticated user profile page with account details" src="https://github.com/user-attachments/assets/177a9d38-e82f-4180-a99b-2c7ad8d38fb1" />

#### Edit Profile

<img width="1040" height="501" alt="Edit profile page with username and email fields" src="https://github.com/user-attachments/assets/3c31d644-11ec-4a86-b1de-fc36cc5d2fc6" />

## Main Pages

| Route                           | Purpose                          |
| ------------------------------- | -------------------------------- |
| `/`                             | Home page with featured products |
| `/auth/login`                   | Login                            |
| `/auth/register`                | Registration                     |
| `/profile`                      | Authenticated user profile       |
| `/profile/edit`                 | Edit authenticated user profile  |
| `/products`                     | Public product catalogue         |
| `/products/{id}`                | Product details and reviews      |
| `/orders/create?productId={id}` | Customer order creation          |
| `/orders/my`                    | Customer order history           |
| `/reviews/new?productId={id}`   | Create review                    |
| `/reviews/{id}/edit`            | Edit own review                  |
| `/admin/products`               | Product management               |
| `/admin/products/new`           | Create product                   |
| `/admin/products/{id}/edit`     | Edit product                     |
| `/admin/orders`                 | Admin order management           |
| `/admin/reviews`                | Admin review moderation          |

## Security Approach

The application uses session-based authentication.

After a successful login, the authenticated user’s identifier is stored in the HTTP session as `user_id`. Access to customer and administrator pages is controlled through session validation and role checks.

The profile feature allows authenticated users to update only their own username and email address. The backend uses the session user ID rather than a client-provided user identifier, so users cannot edit another account through URL manipulation or hidden form values.

Passwords are stored only as BCrypt hashes. Plain-text passwords must never be stored, logged, or committed to the repository.

## Error Handling

The application provides custom Thymeleaf error pages for common browser-facing errors, including:

* 403 Forbidden
* 404 Not Found
* 500 Internal Server Error

Business constraints are enforced in the service layer through custom exceptions. Form validation errors are displayed next to the relevant fields.

## Database Configuration

The application uses MySQL and Spring Data JPA.

Set these environment variables before starting the application:

```text
DB_USERNAME=your_mysql_username
DB_PASSWORD=your_mysql_password
ADMIN_PASSWORD=your_local_admin_password
```

Optional administrator seed overrides:

```text
ADMIN_USERNAME=admin
ADMIN_EMAIL=admin@3dforgemarket.local
```

When `ADMIN_PASSWORD` is configured, the application creates an administrator account only when that username and email do not already exist.

Initial products are seeded only when the products table is empty.

Do not commit database passwords, administrator passwords, API keys, access tokens, or other local secrets.

## Running the Application Locally

1. Install Java 17.
2. Install and start MySQL.
3. Create the local database used by the application.
4. Set the required environment variables.
5. Start the application.

macOS/Linux:

```bash
./mvnw spring-boot:run
```

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Open the application in a browser:

```text
http://localhost:8080
```

## External Image and Model Storage

Product images and public GLB preview models are stored in a public Supabase Storage bucket.

The bucket uses two folders:

```text
images/  → product images
models/  → public GLB preview models
```

The database stores public asset URLs in the `imageUrl` and `modelUrl` fields.

Because the storage bucket is public, only public preview assets should be uploaded. Printable source files and private assets should not be stored there.

## Project Notes

This project was built as an original Spring Fundamentals individual project with a focus on:

* Clear MVC structure
* DTO-based form handling
* Server-side validation
* Custom business exceptions
* Role-based access control
* Simple JPA relationships
* Maintainable package organization
