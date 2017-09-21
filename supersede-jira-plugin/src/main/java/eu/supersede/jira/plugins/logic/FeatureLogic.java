package eu.supersede.jira.plugins.logic;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.event.type.EventDispatchOption;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.priority.Priority;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONObject;
import com.google.gson.JsonObject;

public class FeatureLogic {

	private static FeatureLogic logic;

	private FeatureLogic() {
	}

	public static FeatureLogic getInstance() {
		if (logic == null) {
			logic = new FeatureLogic();
		}
		return logic;
	}
	
	public String sendFeature(HttpServletRequest req, Issue i) {
		ArrayList<String> emptyList = new ArrayList<String>();
		return sendFeature(req, i, emptyList);
	}

	public String sendFeature(HttpServletRequest req, Issue i, ArrayList<String> dependencies) {
		int response = -1;
		String responseData = "";
		try {
			LoginLogic loginLogic = LoginLogic.getInstance();
			String sessionId = loginLogic.login();

			// http://platform.supersede.eu:8280/replan/projects/<ReplanTenant>/features
			URL url = new URL(loginLogic.getReplanHost() + loginLogic.getReplanTenant() + "/features");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");

			JSONObject feature = new JSONObject();
			feature.put("id", i.getId());
			feature.put("name", i.getSummary());
			feature.put("description", i.getDescription());
			feature.put("effort", "100");
			feature.put("deadline", i.getDueDate() != null ? DATE_FORMAT.format(i.getDueDate()) : "");
			feature.put("priority", i.getPriority() != null ? i.getPriority().getName() : "0");
			feature.put("properties", new JSONArray());
			feature.put("required_skills", new JSONArray());
			feature.put("depends_on", new JSONArray());
			JSONArray dependentFeature = new JSONArray();
			for(String dep : dependencies) {
				dependentFeature.put(dep);
			}
			feature.put("hard_dependencies", dependentFeature);
			feature.put("soft_dependencies", new JSONArray());

			JSONArray features = new JSONArray();
			features.put(feature);

			JSONObject container = new JSONObject();
			container.put("features", features);

			conn.setConnectTimeout(LoginLogic.CONN_TIMEOUT);
			conn.setReadTimeout(LoginLogic.CONN_TIMEOUT);
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Accept", "application/json");
			conn.setRequestProperty("Content-Type", "application/json");
			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(conn.getOutputStream());
			outputStreamWriter.write(container.toString());
			outputStreamWriter.flush();

			response = conn.getResponseCode();
			responseData = conn.getResponseMessage();

			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
			StringBuilder sb = new StringBuilder();
			String output;
			while ((output = br.readLine()) != null) {
				sb.append(output);
			}
			return sb.toString();

		} catch (Exception e) {
			e.printStackTrace();
			return e.getMessage();
		}

	}

	public String updateIssueFromFeature(HttpServletRequest req, Issue i) {
		try {
			LoginLogic loginLogic = LoginLogic.getInstance();
			String sessionId = loginLogic.login();

			int response = -1;
			String responseData = null;
			// http://platform.supersede.eu:8280/replan/projects/<ReplanTenant>/features/<id>
			URL url = new URL(loginLogic.getReplanHost() + loginLogic.getReplanTenant() + "/features/");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(LoginLogic.CONN_TIMEOUT);
			conn.setReadTimeout(LoginLogic.CONN_TIMEOUT);
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");

			response = conn.getResponseCode();
			responseData = conn.getResponseMessage();

			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
			StringBuilder sb = new StringBuilder();
			String output;
			while ((output = br.readLine()) != null) {
				sb.append(output);
			}
			SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");
			JSONObject feature = null;
			JSONArray array = new JSONArray(sb.toString());
			for (int j = 0; j < array.length(); j++) {
				// For every request, I create a custom Alert with significant
				// fields inside
				// It is a custom object created for JIRA, because I cannot use
				// linked projects or libraries.
				JSONObject f = array.getJSONObject(j);
				if (i.getId().toString().equals(f.getString("code"))) {
					feature = f;
					break;
				}
			}

			if (feature == null) {
				return "No feature has the requested code";
			}

			// If there is "error" field, the issue does not exist
			boolean error = feature.has("error");
			if (error) {
				return "Feature not found. You have to export them before importing back";
			}

			String issueKey = i.getKey();
			IssueManager issueManager = ComponentAccessor.getIssueManager();
			MutableIssue mIssue = issueManager.getIssueByKeyIgnoreCase(issueKey);
			mIssue.setSummary(feature.getString("name"));
			mIssue.setDescription(feature.getString("description"));
			// calculate time
			Calendar cal = Calendar.getInstance();
			String deadline = feature.getString("deadline");
			if (deadline != null && !deadline.equals("null")) {
				cal.setTime(DATE_FORMAT.parse(feature.getString("deadline")));
				mIssue.setDueDate(new Timestamp(cal.getTimeInMillis()));
			}
			mIssue.setPriorityId(feature.getString("priority"));

			issueManager.updateIssue(loginLogic.getCurrentUser(), mIssue, EventDispatchOption.ISSUE_UPDATED, true);
			return "Issue " + i.getKey() + " successfully updated";

		} catch (Exception e) {
			e.printStackTrace();
			return "Error on issue " + i.getKey() + " " + e.getMessage();
		}
	}

}
