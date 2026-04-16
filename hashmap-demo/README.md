# HashMap Memory Leak Demo

This project demonstrates a memory leak issue caused by improper implementation of `equals()` and `hashCode()` methods in a class used as a key in a HashMap. It also includes implementations of Caffeine local cache and Spring Cache + Caffeine.

## Problem Description

The issue occurs when:
1. A class is used as a key in a HashMap
2. The class doesn't override `equals()` and `hashCode()` methods
3. Multiple instances of the class with the same logical value are created
4. Each instance is treated as a different key, causing the HashMap to grow indefinitely

## Project Structure

- `OrderController.java`: Demonstrates the memory leak issue
- `FixedOrderController.java`: Shows the fixed version with proper `equals()` and `hashCode()`
- `MemoryLeakTest.java`: Test class to reproduce the memory leak
- `FixedMemoryLeakTest.java`: Test class to verify the fix
- `CacheConfig.java`: Caffeine cache configuration
- `CaffeineCacheController.java`: Spring Cache + Caffeine implementation
- `DirectCaffeineCacheController.java`: Direct Caffeine cache implementation

## How to Run

1. Build the project:
   ```bash
   mvn clean package
   ```

2. Run the Spring Boot application:
   ```bash
   java -jar target/hashmap-demo-1.0-SNAPSHOT.jar
   ```

3. Run the memory leak test:
   ```bash
   java -cp target/hashmap-demo-1.0-SNAPSHOT.jar com.example.hashmap.MemoryLeakTest
   ```

4. Run the fixed memory leak test:
   ```bash
   java -cp target/hashmap-demo-1.0-SNAPSHOT.jar com.example.hashmap.FixedMemoryLeakTest
   ```

## Endpoints

### Memory Leak Demo
- `POST /api/orders`: Create order
- `GET /api/orders/{orderId}`: Get order
- `GET /api/orders/cache/size`: Get cache size

### Fixed Version
- `POST /api/fixed-orders`: Create order
- `GET /api/fixed-orders/{orderId}`: Get order
- `GET /api/fixed-orders/cache/size`: Get cache size

### Spring Cache + Caffeine
- `POST /api/caffeine-orders`: Create order
- `GET /api/caffeine-orders/{orderId}`: Get order
- `PUT /api/caffeine-orders/{orderId}`: Update order
- `DELETE /api/caffeine-orders/{orderId}`: Delete order
- `GET /api/caffeine-orders/cache/clear`: Clear cache

### Direct Caffeine Cache
- `POST /api/direct-caffeine-orders`: Create order
- `GET /api/direct-caffeine-orders/{orderId}`: Get order
- `PUT /api/direct-caffeine-orders/{orderId}`: Update order
- `DELETE /api/direct-caffeine-orders/{orderId}`: Delete order
- `GET /api/direct-caffeine-orders/cache/stats`: Get cache statistics
- `GET /api/direct-caffeine-orders/cache/clear`: Clear cache

## Expected Results

- **Memory Leak Test**: The cache size will keep growing even though we're using the same orderId
- **Fixed Memory Leak Test**: The cache size will remain 1, as the same orderId is properly recognized
- **Caffeine Cache Tests**: Orders will be cached with proper expiration and statistics

## Key Fix

The fix is to properly implement `equals()` and `hashCode()` methods in the `FixedOrder` class:

```java
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    FixedOrder that = (FixedOrder) o;
    return Objects.equals(orderId, that.orderId);
}

@Override
public int hashCode() {
    return Objects.hash(orderId);
}
```

This ensures that objects with the same `orderId` are considered equal, regardless of other fields like `customerId`.

## Caffeine Cache Features

- **Automatic expiration**: Entries expire after 10 minutes of access or 1 hour of creation
- **Size limit**: Maximum 1000 entries
- **Statistics**: Cache hit/miss rates, eviction counts
- **Thread-safe**: Safe for concurrent access
- **Spring Cache integration**: Easy to use with @Cacheable, @CachePut, @CacheEvict annotations
