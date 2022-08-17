package reliqreports.common;

public enum EReportCategory {
    ORGANIZATION("12"),
    PATIENT("10"),
    ORGANIZTION_ZIP("13");
    
    public String category;
    
    private EReportCategory(String category) {
        this.category = category;
    }
    
}
