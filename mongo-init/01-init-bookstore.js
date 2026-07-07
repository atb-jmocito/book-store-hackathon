// Initialize the bookstore database with the static categories used by the API.
const bookstoreDb = db.getSiblingDB("bookstore");

bookstoreDb.createCollection("categories");
bookstoreDb.createCollection("authors");

bookstoreDb.categories.insertMany([
  { id: 1, name: "Fiction" },
  { id: 2, name: "Non-Fiction" },
  { id: 3, name: "Science" },
  { id: 4, name: "Biography" },
  { id: 5, name: "Children" },
  { id: 6, name: "Programming" }
]);

bookstoreDb.categories.createIndex({ id: 1 }, { unique: true });
bookstoreDb.authors.createIndex({ nameKey: 1 }, { unique: true });
