db = db.getSiblingDB('orders_db');

// Create root user for the database
db.createUser({
  user: 'root',
  pwd: 'example',
  roles: [{ role: 'readWrite', db: 'orders_db' }]
});

// Create 'products' collection and insert sample data
db.createCollection('products');
db.products.insertMany([
  {
    productId: 'product-101',
    name: 'Laptop',
    description: 'A high-performance laptop',
    price: 1200.00
  },
  {
    productId: 'product-102',
    name: 'Smartphone',
    description: 'A feature-rich smartphone',
    price: 800.00
  },
  {
    productId: 'product-103',
    name: 'Headphones',
    description: 'Noise-cancelling headphones',
    price: 200.00
  }
]);

// Create 'clients' collection and insert sample data
db.createCollection('clients');
db.clients.insertMany([
  {
    customerId: 'customer-001',
    name: 'Alice Johnson',
    email: 'alice@example.com',
    isActive: true
  },
  {
    customerId: 'customer-002',
    name: 'Bob Smith',
    email: 'bob@example.com',
    isActive: false
  },
  {
    customerId: 'customer-003',
    name: 'Charlie Brown',
    email: 'charlie@example.com',
    isActive: true
  }
]);

// Create 'orders' collection and insert sample data
db.createCollection('orders');
db.orders.insertMany([
  {
    orderId: 'order-001',
    customerId: 'customer-001',
    products: [
      {
        productId: 'product-101',
        name: 'Laptop',
        price: 1200.00
      },
      {
        productId: 'product-103',
        name: 'Headphones',
        price: 200.00
      }
    ],
    totalAmount: 1400.00,
    status: 'pending'
  },
  {
    orderId: 'order-002',
    customerId: 'customer-003',
    products: [
      {
        productId: 'product-102',
        name: 'Smartphone',
        price: 800.00
      }
    ],
    totalAmount: 800.00,
    status: 'completed'
  }
]);