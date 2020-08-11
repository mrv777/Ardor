package com.jelurida.ardor.contracts;

import nxt.BlockchainTest;
import nxt.blockchain.Chain;
import nxt.http.callers.ExchangeCoinsCall;
import nxt.http.responses.CoinExchangeOrderResponse;
import org.json.simple.JSONObject;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

import static nxt.blockchain.ChildChain.AEUR;
import static nxt.blockchain.ChildChain.IGNIS;
import static nxt.blockchain.FxtChain.FXT;

public class OrderBean {
    final long price;
    final long quantity;
    final Chain from;
    final Chain to;

    public OrderBean(long price, long quantity, Chain from, Chain to) {
        this.price = price;
        this.quantity = quantity;
        this.from = from;
        this.to = to;
    }

    public OrderBean(CoinExchangeOrderResponse order) {
        price = order.getBidNQTPerCoin();
        quantity = order.getQuantityQNT();
        from = Chain.getChain(order.getChain());
        to = Chain.getChain(order.getExchangeChain());
    }

    public static OrderBean order(long priceCents, long quantityCoins, Chain from, Chain to) {
        return new OrderBean(priceCents * getCents(from), quantityCoins * getCoin(to), from, to);
    }

    public static OrderBean order(double priceUnits, long quantityCoins, Chain from, Chain to) {
        return new OrderBean(Math.round(priceUnits * getCoin(from)), quantityCoins * getCoin(to), from, to);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", OrderBean.class.getSimpleName() + "[", "]")
                .add("price=" + getPriceString())
                .add("quantity=" + getQuantityString())
                .add("from=" + from)
                .add("to=" + to)
                .toString();
    }

    private String getQuantityString() {
        long coin = getCoin(to);
        if (quantity % coin == 0) {
            return (quantity / coin) + " * " + to.getName() + ".ONE_COIN";
        } else {
            return "" + quantity;
        }
    }

    private String getPriceString() {
        long cents = getCents(from);
        if (price % cents == 0) {
            return (price / cents) + " * " + from.getName() + ".CENTS";
        } else {
            return "" + price;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderBean orderBean = (OrderBean) o;
        return price == orderBean.price &&
                quantity == orderBean.quantity &&
                from.equals(orderBean.from) &&
                to.equals(orderBean.to);
    }

    public boolean equalsIgnoreQuantity(OrderBean other) {
        return price == other.price &&
                from.equals(other.from) &&
                to.equals(other.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(price, quantity, from, to);
    }

    private static long getCents(Chain chain) {
        return getCoin(chain) / 100;
    }

    private static long getCoin(Chain chain) {
        if (chain == FXT) {
            return FXT.ONE_COIN;
        }
        if (chain == IGNIS) {
            return IGNIS.ONE_COIN;
        }
        if (chain == AEUR) {
            return AEUR.ONE_COIN;
        }
        throw new IllegalArgumentException("Not implemented for chain: " + chain);
    }

    public long getPrice() {
        return price;
    }

    public long getQuantity() {
        return quantity;
    }

    public Chain getFrom() {
        return from;
    }

    public Chain getTo() {
        return to;
    }

    public static void createOrderBook(List<OrderBean> orders) {
        orders.forEach(order -> createExchangeOrder(order.getQuantity(), order.getFrom(), order.getPrice(), order.getTo()));
    }

    private static void createExchangeOrder(long quantity, Chain fromChain, long price, Chain toChain) {
        long feeNQT = fromChain == FXT || toChain == FXT
                ? FXT.ONE_COIN / 2
                : fromChain.ONE_COIN;
        JSONObject response = ExchangeCoinsCall.create(fromChain.getId())
                .exchange(toChain.getId())
                .quantityQNT(quantity)
                .priceNQTPerCoin(price)
                .secretPhrase(BlockchainTest.DAVE.getSecretPhrase())
                .feeNQT(feeNQT)
                .build().invokeNoError();
        System.out.println(response);
    }
}
