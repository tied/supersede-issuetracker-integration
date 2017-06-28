package eu.supersede.jira.plugins.logic;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.bc.project.ProjectService;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONObject;

import eu.supersede.jira.plugins.servlet.Alert;

public class AlertLogic {
	
	private static AlertLogic logic;
	
	private LoginLogic loginLogic;

	private static final Logger log = LoggerFactory.getLogger(AlertLogic.class);


	private AlertLogic() {
		loginLogic = LoginLogic.getInstance();
	}

	public static AlertLogic getInstance() {
		if (logic == null) {
			logic = new AlertLogic();
		}
		return logic;
	}
	
	public List<Alert> fetchAlerts(HttpServletRequest req) {
		// retrieves a list of all alerts on SS
		return fetchAlerts(req, "");
	}

	public List<Alert> fetchAlerts(HttpServletRequest req, String alertId) {
		List<Alert> alerts = new LinkedList<Alert>();
		try {
			// retrieve the list of all alerts from the specified tenant
			String sessionId = loginLogic.login();
			if(alertId != null && !alertId.isEmpty()){
				alertId = "?id="+alertId;
			} else{
				alertId = "";
			}
			URL url = new URL(loginLogic.getUrl() + "/supersede-dm-app/alerts" + alertId);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(LoginLogic.CONN_TIMEOUT);
			conn.setReadTimeout(LoginLogic.CONN_TIMEOUT);
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");
			conn.setRequestProperty("Authorization", loginLogic.getBasicAuth());
			conn.setRequestProperty("TenantId", loginLogic.getCurrentProject());
			conn.setRequestProperty("Cookie", "SESSION=" + sessionId + ";");

			log.debug("connection code " + conn.getResponseCode());

			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

			String output;
			StringBuffer sb = new StringBuffer();
			while ((output = br.readLine()) != null) {
				sb.append(output);
			}
			JSONArray jarr = new JSONArray(sb.toString());
			int l = jarr.length();
			for (int i = 0; i < l; ++i) {
				JSONObject o = jarr.getJSONObject(i);
				try {
					// We retrieve a list of alerts because there could be more
					// than one request per alert.
					// Every request could have different descriptions.
					List<Alert> a = parseJSONAsAlert(o);
					alerts.addAll(a);
				} catch (Exception e) {
					log.error("parsing ", o);
				}
			}

			conn.disconnect();
		} catch (Exception e) {
			log.error(e.getMessage());
		}

		return alerts;
	}

	private List<Alert> parseJSONAsAlert(JSONObject o) {
		List<Alert> al = new LinkedList<Alert>();
		try {
			// Retrieval of requests linked to every alert
			JSONArray requests = o.getJSONArray("requests");
			for (int i = 0; i < requests.length(); i++) {
				// For every request, I create a custom Alert with significant
				// fields inside
				// It is a custom object created for JIRA, because I cannot use
				// linked projects or libraries.
				JSONObject r = requests.getJSONObject(i);
				Alert a = new Alert();
				a.setApplicationId(o.getString("applicationId"));
				a.setId(o.getString("id"));
				a.setTenant(o.getString("tenant"));
				Date d = new Date(/* o.getLong("timestamp") */);
				a.setTimestamp(d.toString());
				a.setDescription(r.getString("description"));
				al.add(a);
			}

		} catch (Exception e) {
			log.error(e.getMessage());
		}
		return al;
	}
}
