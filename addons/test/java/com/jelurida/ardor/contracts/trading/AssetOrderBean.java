package com.jelurida.ardor.contracts.trading;

import java.util.StringJoiner;

abstract class AssetOrderBean {

    final long price;
    final long quantity;
    final long asset;

    AssetOrderBean(long price, long quantity, long asset) {
        this.price = price;
        this.quantity = quantity;
        this.asset = asset;
    }

    static AssetOrderBean assetBuy(long price, long quantity, long asset) {
        return new AssetBuyBean(price, quantity, asset);
    }

    static AssetOrderBean assetSell(long price, long quantity, long asset) {
        return new AssetSellBean(price, quantity, asset);
    }

    abstract void placeOrder();

    @Override
    public String toString() {
        return new StringJoiner(", ", getClass().getSimpleName() + "[", "]")
                .add("price=" + price)
                .add("quantity=" + quantity)
                .add("asset=" + asset)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AssetOrderBean that = (AssetOrderBean) o;

        if (price != that.price) return false;
        if (quantity != that.quantity) return false;
        return asset == that.asset;
    }

    @Override
    public int hashCode() {
        int result = (int) (price ^ (price >>> 32));
        result = 31 * result + (int) (quantity ^ (quantity >>> 32));
        result = 31 * result + (int) (asset ^ (asset >>> 32));
        return result;
    }


}
