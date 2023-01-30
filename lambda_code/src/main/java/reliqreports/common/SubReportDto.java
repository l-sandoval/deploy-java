package reliqreports.common;

public class SubReportDto {
    
    private String subReportName;
    private String subReportSheetName;
    private String subReportFilePath;
    
    public SubReportDto(String subReportName, String subReportSheetName, String subReportFilePath) {
        super();
        this.subReportName = subReportName;
        this.subReportSheetName = subReportSheetName;
        this.subReportFilePath = subReportFilePath;
    }

    public String getSubReportName() {
        return subReportName;
    }

    public void setSubReportName(String subReportName) {
        this.subReportName = subReportName;
    }

    public String getSubReportSheetName() {
        return subReportSheetName;
    }

    public void setSubReportSheetName(String subReportSheetName) {
        this.subReportSheetName = subReportSheetName;
    }

    public String getSubReportFilePath() {
        return subReportFilePath;
    }

    public void setSubReportFilePath(String subReportFilePath) {
        this.subReportFilePath = subReportFilePath;
    }
}
