package reliqreports.common.enums;

public enum EReportCategory {
    ORGANIZATION("12"),
    PATIENT("10");
    
    public String category;
    
    private EReportCategory(String category) {
        this.category = category;
    }
    
}
