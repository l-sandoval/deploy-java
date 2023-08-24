package reliqreports.common.enums;

public enum EReportCategory {
    ORGANIZATION("12"),
    PATIENT("10"),
    ORGANIZATION_ZIP("13"),
    API_DELIVERY("14");
    
    public String category;
    
    private EReportCategory(String category) {
        this.category = category;
    }
    
}
