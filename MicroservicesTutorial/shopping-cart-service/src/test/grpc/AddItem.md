```shell
grpcurl -d '{"cartId":"cart3", "itemId":"hoodie", "quantity":5}' -plaintext 127.0.0.1:8101 shoppingcart.ShoppingCartService.AddItem
```
