package reliqreports.common.enums;

public enum EProcessCategory {

    UPLOAD("UPLOAD"),
    ORGANIZATION_ZIP("ORGANIZATION_ZIP"),
    API_DELIVERY("API_DELIVERY"),
    BILLING_ZIP("BILLING_ZIP");

    public final String category;

    private EProcessCategory(String category) {
        this.category = category;
    }
}
