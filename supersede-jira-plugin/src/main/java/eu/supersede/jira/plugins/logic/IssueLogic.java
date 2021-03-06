/*
   (C) Copyright 2015-2018 The SUPERSEDE Project Consortium

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
     http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package eu.supersede.jira.plugins.logic;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.IssueService.IssueResult;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.bc.project.ProjectService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.attachment.CreateAttachmentParamsBean;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.jql.builder.JqlClauseBuilder;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.jira.web.util.AttachmentException;
import com.atlassian.query.Query;

import eu.supersede.jira.plugins.servlet.Alert;
import eu.supersede.jira.plugins.servlet.Difference;
import eu.supersede.jira.plugins.servlet.Requirement;
import eu.supersede.jira.plugins.servlet.XMLFileGenerator;

public class IssueLogic {

	private static IssueLogic logic;

	private LoginLogic loginLogic;

	private RequirementLogic requirementsLogic;

	private IssueService issueService;

	private ProjectService projectService;

	private SearchService searchService;

	private static final Logger log = LoggerFactory.getLogger(IssueLogic.class);

	private IssueLogic(IssueService issueService, ProjectService projectService, SearchService searchService) {
		loginLogic = LoginLogic.getInstance();
		requirementsLogic = RequirementLogic.getInstance(issueService, projectService, searchService);
		this.issueService = issueService;
		this.projectService = projectService;
		this.searchService = searchService;
	}

	public static IssueLogic getInstance(IssueService issueService, ProjectService projectService,
			SearchService searchService) {
		if (logic == null) {
			logic = new IssueLogic(issueService, projectService, searchService);
		}
		return logic;
	}

	public List<Issue> getIssues(HttpServletRequest req, Long supersedeFieldId) {
		return getIssues(req, supersedeFieldId, null);
	}

	public IssueResult getIssue(ApplicationUser user, String issueKey) {
		return issueService.getIssue(user, issueKey);
	}

	public List<Issue> getIssuesFromFilter(HttpServletRequest req, Query query) {
		ApplicationUser user = loginLogic.getCurrentUser();

		PagerFilter pagerFilter = PagerFilter.getUnlimitedFilter();
		SearchResults searchResults = null;
		try {
			// Perform search results
			searchResults = searchService.search(user, query, pagerFilter);
		} catch (SearchException e) {
			e.printStackTrace();
		}
		// return the results
		return searchResults.getIssues();
	}

	/**
	 * Retrieve an issue given its key NOT PROJECT-WISE
	 * 
	 * @param id
	 * @return
	 */
	public Issue getIssueByKey(String issueKey) {
		ApplicationUser user = loginLogic.getCurrentUser();
		JqlClauseBuilder jqlClauseBuilder = JqlQueryBuilder.newClauseBuilder();
		jqlClauseBuilder.field("key").eq(issueKey);
		Query query = jqlClauseBuilder.buildQuery();
		PagerFilter pagerFilter = PagerFilter.getUnlimitedFilter();
		com.atlassian.jira.issue.search.SearchResults searchResults = null;
		try {
			searchResults = searchService.search(user, query, pagerFilter);
		} catch (SearchException e) {
			e.printStackTrace();
		}
		// It must be 0 or 1
		List<Issue> list = searchResults.getIssues();

		return list.size() == 1 ? list.get(0) : null;
	}

	public Issue getIssueById(String issueId) {
		ApplicationUser user = loginLogic.getCurrentUser();
		JqlClauseBuilder jqlClauseBuilder = JqlQueryBuilder.newClauseBuilder();
		jqlClauseBuilder.field("id").eq(issueId);
		Query query = jqlClauseBuilder.buildQuery();
		PagerFilter pagerFilter = PagerFilter.getUnlimitedFilter();
		com.atlassian.jira.issue.search.SearchResults searchResults = null;
		try {
			searchResults = searchService.search(user, query, pagerFilter);
		} catch (SearchException e) {
			e.printStackTrace();
		}
		// It must be 0 or 1
		List<Issue> list = searchResults.getIssues();

		return list.size() == 1 ? list.get(0) : null;
	}

	/**
	 * Retrieve the issues with a valid supersede field set
	 * 
	 * @param req
	 * @return
	 */
	public List<Issue> getIssues(HttpServletRequest req, Long supersedeFieldId, String id) {
		// User is required to carry out a search
		ApplicationUser user = loginLogic.getCurrentUser();

		// search issues

		// The search interface requires JQL clause... so let's build one
		JqlClauseBuilder jqlClauseBuilder = JqlQueryBuilder.newClauseBuilder();
		// Our JQL clause is simple project="TUTORIAL"
		// com.atlassian.query.Query query =
		// jqlClauseBuilder.project("TEST").buildQuery();

		// Build the basic Jql query
		List<Project> projects = ComponentAccessor.getProjectManager().getProjectObjects();
		// jqlClauseBuilder.customField(supersedeFieldId).isNotEmpty().and().project(

		jqlClauseBuilder.project(
				req.getParameter("projectField") != null ? req.getParameter("projectField") : projects.get(0).getKey());
		if (id != null && !"".equals(id) && !" ".equals(id)) {
			// if an ID is provided, use in in filter
			// ID MUST BE the beginnning of the string. You cannot put a
			// wildcard at the beginning of the search
			jqlClauseBuilder.and().sub().customField(supersedeFieldId)
					.like(id.substring(0, id.length() > 255 ? 254 : id.length() - 1)).or().field("key").eq(id).or()
					.field("summary").like(id + "*").endsub();
		}
		Query query = jqlClauseBuilder.buildQuery();
		// A page filter is used to provide pagination. Let's use an unlimited
		// filter to
		// to bypass pagination.
		PagerFilter pagerFilter = PagerFilter.getUnlimitedFilter();
		com.atlassian.jira.issue.search.SearchResults searchResults = null;
		try {
			// Perform search results
			searchResults = searchService.search(user, query, pagerFilter);
		} catch (SearchException e) {
			e.printStackTrace();
		}
		// return the results
		return searchResults.getIssues();
	}

	public List<Issue> getAllIssues(HttpServletRequest req, Long supersedeFieldId) {
		ApplicationUser user = loginLogic.getCurrentUser();
		JqlClauseBuilder jqlClauseBuilder = JqlQueryBuilder.newClauseBuilder();
		jqlClauseBuilder.customField(supersedeFieldId).isNotEmpty();
		Query query = jqlClauseBuilder.buildQuery();
		PagerFilter pagerFilter = PagerFilter.getUnlimitedFilter();
		com.atlassian.jira.issue.search.SearchResults searchResults = null;
		try {
			// Perform search results
			searchResults = searchService.search(user, query, pagerFilter);
		} catch (SearchException e) {
			e.printStackTrace();
		}
		// return the results
		return searchResults.getIssues();
	}

	public Issue getIssueByRequirement(ApplicationUser user, Long supersedeFieldId, String requirementId) {
		// search issues
		// The search interface requires JQL clause... so let's build one
		JqlClauseBuilder jqlClauseBuilder = JqlQueryBuilder.newClauseBuilder();
		// Our JQL clause is simple project="TUTORIAL"
		// com.atlassian.query.Query query =
		// jqlClauseBuilder.project("TEST").buildQuery();
		Query query = jqlClauseBuilder.customField(supersedeFieldId).like(requirementId).and()
				.project(loginLogic.getCurrentProject()).buildQuery();
		log.debug(query.getQueryString());
		log.debug(query.getWhereClause().toString());
		// A page filter is used to provide pagination. Let's use an unlimited
		// filter to
		// to bypass pagination.
		PagerFilter pagerFilter = PagerFilter.getUnlimitedFilter();
		com.atlassian.jira.issue.search.SearchResults searchResults = null;
		try {
			// Perform search results
			searchResults = searchService.search(user, query, pagerFilter);
		} catch (SearchException e) {
			e.printStackTrace();
		}
		// return the results
		List<Issue> issues = searchResults.getIssues();
		if (0 == issues.size()) {
			log.debug("no issues found for requirement " + requirementId);
			return null;
		}
		if (1 < issues.size()) {
			log.warn("more issues mapped to the same requirement " + requirementId + ": returning the first found");
		}
		return issues.get(0);
	}

	public IssueResult newIssue(HttpServletRequest req, String name, String description, String id,
			Collection<String> errors, CustomField supersedeField, String projectId, String typeId) {
		IssueResult issue = null;
		ApplicationUser user = loginLogic.getCurrentUser();
		IssueInputParameters issueInputParameters = issueService.newIssueInputParameters();
		issueInputParameters.setDescription(description);
		issueInputParameters.addCustomFieldValue(supersedeField.getId(),
				id.substring(0, id.length() > 255 ? 254 : id.length() - 1));
		issueInputParameters.setReporterId(user.getName());
		Project project = projectService
				.getProjectByKey(user, /* loginLogic.getCurrentProject().toUpperCase() */ projectId).getProject();
		if (null == project) {
			errors.add(
					"Cannot add issue for requirement " + id + ": no such project " + loginLogic.getCurrentProject());
		} else {
			issueInputParameters.setProjectId(project.getId());
			String checkedType = typeId != null && !typeId.isEmpty() ? typeId
					: project.getIssueTypes().iterator().next().getId();
			issueInputParameters.setIssueTypeId(checkedType);

			Iterator<IssueType> typeIterator = project.getIssueTypes().iterator();
			while (typeIterator.hasNext()) {
				IssueType t = typeIterator.next();
				if (!t.getId().equals(checkedType)) {
					continue;
				}
				issueInputParameters.setSummary("[" + t.getName() + "] " + name);

				break;
			}

			// Perform the validation
			issueInputParameters.setSkipScreenCheck(true);
			IssueService.CreateValidationResult result = issueService.validateCreate(user, issueInputParameters);

			if (result.getErrorCollection().hasAnyErrors()) {
				Map<String, String> errorsMap = result.getErrorCollection().getErrors();
				for (String eid : errorsMap.keySet()) {
					errors.add(eid + ": " + errorsMap.get(eid));
				}
				log.error("cannot add issue for requirement " + id);
			} else {
				issue = issueService.create(user, result);
				log.info("added issue for requirement " + id);
			}
		}
		return issue;
	}

	public void updateIssue(MutableIssue issue, ApplicationUser user, String requirementId, Collection<String> errors,
			CustomField supersedeField) {

		issue.setCustomFieldValue(supersedeField, requirementId);
		Object customField = issue.getCustomFieldValue(supersedeField);
		log.debug("custom field of " + issue.getKey() + " set to " + customField);

		IssueInputParameters issueInputParameters = issueService.newIssueInputParameters();
		issueInputParameters.addCustomFieldValue(supersedeField.getId(), requirementId);
		IssueService.UpdateValidationResult updateRes = issueService.validateUpdate(user, issue.getId(),
				issueInputParameters);

		if (updateRes.getErrorCollection().hasAnyErrors()) {
			Map<String, String> errorsMap = updateRes.getErrorCollection().getErrors();
			for (String eid : errorsMap.keySet()) {
				errors.add(eid + ": " + errorsMap.get(eid));
			}
			log.error("cannot update issue for requirement " + requirementId);
		} else {
			IssueResult updated = issueService.update(user, updateRes);
			log.info("updated issue " + issue.getId() + " for requirement " + requirementId);

			Object updatedField = updated.getIssue().getCustomFieldValue(supersedeField);
			log.debug("updated custom field: ", updatedField);
		}
	}

	public void attachToIssue(Alert source, Issue target) {
		// If "Attach" button was clicked in alert table
		XMLFileGenerator xml = new XMLFileGenerator(source);
		File tmpFile = xml.generateXMLFile();
		if (tmpFile == null) {
			return;
		}

		CreateAttachmentParamsBean capb = new CreateAttachmentParamsBean.Builder(tmpFile, source.getId() + ".xml",
				"application/xml", null, target).build();
		try {
			ComponentAccessor.getAttachmentManager().createAttachment(capb);
		} catch (AttachmentException e) {
			e.printStackTrace();
		}
	}

	public List<Difference> compareIssues(HttpServletRequest req, Long supersedeFieldId, CustomField supersedeField) {
		List<Issue> JIRAissues = getIssues(req, supersedeFieldId);
		List<Requirement> requirements = new LinkedList<Requirement>();
		List<Difference> differences = new LinkedList<Difference>();
		requirementsLogic.getRequirements(req, requirements, false, supersedeFieldId);

		// ricerco gli ID jira nella lista requirements in modo da inserirli
		// come anomalie
		System.out.println("####### I RETRIEVED " + JIRAissues.size() + " JIRA Issues");
		System.out.println("####### I RETRIEVED " + requirements.size() + " SS Issues");
		log.error("####### I RETRIEVED " + JIRAissues.size() + " JIRA Issues");
		log.error("####### I RETRIEVED " + requirements.size() + " SS Issues");
		for (Issue i : JIRAissues) {

			for (Requirement r : requirements) {
				String value = (String) i.getCustomFieldValue(supersedeField);
				log.error("VALUES " + String.valueOf(value) + " " + r.getId());
				if (String.valueOf(value).equals(r.getId())) {
					// Verifico la coerenza dei dati
					boolean equal = true;
					equal &= i.getDescription().equals(r.getDescription());
					if (!equal) {
						log.error("####### I RETRIEVED AN ISSUE THAT NEEDS TO BE SHOWN");
						Difference d = new Difference();
						d.setAnomalyType("DESCRIPTION");
						d.setId(r.getId());
						d.setJIRAValue(i.getDescription());
						d.setSSValue(r.getDescription());
						differences.add(d);
					}

				}
			}
		}
		return differences;
	}

	public Collection<IssueType> getIssueTypesByProject(String projectKey) {
		Project proj = ComponentAccessor.getProjectManager().getProjectByCurrentKey(projectKey);
		Collection<IssueType> issueTypes = ComponentAccessor.getIssueTypeSchemeManager().getIssueTypesForProject(proj);
		ArrayList<IssueType> filteredIssueTypes = new ArrayList<IssueType>();

		// Remove subtypes from list (you cannot create a subtask from outside
		// an issue, so it doesn't make sense to include it
		Iterator<IssueType> iter = issueTypes.iterator();
		https: // open.spotify.com/track/5W3cjX2J3tjhG8zb6u0qHn
		while (iter.hasNext()) {
			IssueType it = iter.next();
			if (!it.isSubTask()) {
				filteredIssueTypes.add(it);
			}
		}

		return filteredIssueTypes;
	}

	public List<String> checkSimilarity(Alert a, List<Issue> issues, HttpServletRequest req) {
		try {
			int response = -1;
			String responseData = "";

			String sessionId = loginLogic.login();
			String xsrf = loginLogic.authenticate(sessionId);
			HttpSession session = req.getSession();
			session.setAttribute("Cookie", "SESSION=" + sessionId + ";");
			URL url = new URL(loginLogic.getSimilarity());
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(LoginLogic.CONN_TIMEOUT);
			conn.setReadTimeout(LoginLogic.CONN_TIMEOUT);
			conn.setRequestProperty("Authorization", loginLogic.getBasicAuth());
			conn.setRequestProperty("TenantId", loginLogic.getCurrentProject());
			conn.setRequestProperty("Cookie", "SESSION=" + sessionId + ";");
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestProperty("X-XSRF-TOKEN", xsrf);

			JSONObject similarity = new JSONObject();
			JSONObject feedback = new JSONObject();
			feedback.put("text", a.getDescription());
			similarity.put("k", Math.min(Integer.parseInt(req.getParameter("similarity-number")), issues.size()));
			similarity.put("feedback", feedback);
			similarity.put("tenant", loginLogic.getCurrentProject());
			similarity.put("language", "en");

			JSONArray requirements = new JSONArray();
			for (Issue i : issues) {
				JSONObject requirement = new JSONObject();
				requirement.put("_id", i.getId());
				requirement.put("title", i.getSummary() != null ? i.getSummary().replaceAll("[^A-Za-z0-9 ]", "") : "");
				requirement.put("description",
						i.getDescription() != null ? i.getDescription().replaceAll("[^A-Za-z0-9 ]", "") : "");

				requirements.put(requirement);
			}

			similarity.put("requirements", requirements);

			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(conn.getOutputStream());
			outputStreamWriter.write(similarity.toString());
			outputStreamWriter.flush();

			response = conn.getResponseCode();
			System.out.println(response);
			responseData = conn.getResponseMessage();

			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
			StringBuilder sb = new StringBuilder();
			String output;
			while ((output = br.readLine()) != null) {
				sb.append(output);
			}
			System.out.println(sb.toString());
			JSONArray result = new JSONArray(sb.toString());
			List<String> similarityList = new ArrayList<String>();
			int l = result.length();
			for (int i = 0; i < l; ++i) {
				JSONObject o = result.getJSONObject(i);

				IssueManager im = ComponentAccessor.getIssueManager();
				Long id = Long.parseLong(o.getString("id"));
				Issue sim = im.getIssueObject(id);
				similarityList.add(sim.getKey() + " - " + sim.getSummary() + "plugins/servlet/supersede-alerts?projectField=ASVP&selectionList=" + a.getBase64Id()
						+ "&issuesSelectionList=" + sim.getKey() + "&action=Attach");
			}

			return similarityList;

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}
}
