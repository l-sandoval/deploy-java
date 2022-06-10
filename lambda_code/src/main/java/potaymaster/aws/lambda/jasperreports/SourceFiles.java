package potaymaster.aws.lambda.jasperreports;

public class SourceFiles {
	public String csv;
	public String template;
	
	public SourceFiles(String csv, String template) {
		this.csv = csv;
		this.template = template;
	}

	public String getCsv() {
		return csv;
	}

	public void setCsv(String csv) {
		this.csv = csv;
	}

	public String getTemplate() {
		return template;
	}

	public void setTemplate(String template) {
		this.template = template;
	}
	
	
}
