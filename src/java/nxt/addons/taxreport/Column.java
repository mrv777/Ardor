package nxt.addons.taxreport;

enum Column {
    TYPE("Type"),
    BUY_VOLUME("Buy"),
    BUY_CURRENCY("Cur."),
    SELL_VOLUME("Sell"),
    SELL_CURRENCY("Cur."),
    FEE_VOLUME("Fee"),
    FEE_CURRENCY("Cur."),
    EXCHANGE("Exchange"),
    GROUP("Group"),
    COMMENT("Comment"),
    DATE("Date");

    private final String label;

    Column(String label) {
        this.label = label;
    }

    String getLabel() {
        return label;
    }
}
