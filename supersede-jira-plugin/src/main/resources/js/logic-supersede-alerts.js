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

AJS.$(document).ready(
		function() {
			$('#project-select-import').val($('.projectField').val()).trigger(
					'change');
			$('#project-select-bulk').val($('.projectField').val()).trigger(
					'change');
			$('#project-select-attach').val($('.projectField').val()).trigger(
					'change');
			$('#page-title').text('Alerts Management');
			onPageLoad();
		});

function onPageLoad() {
	AJS.$('#project-select-import').auiSelect2();
	AJS.$('#project-select-bulk').auiSelect2();
	AJS.$('#project-select-attach').auiSelect2();
	AJS.$('#issue-type-selector').auiSelect2();
	$('#issue-type-selector').on('change', issueTypeSelectorChange);
	$(".toEnable").prop('disabled', true);
	$(".toEnableDialog").prop('disabled', true);
	var selectionString = '';
	var issuesSelectionString = '';

	jQuery('.check-alerts').click(function() {
		var self = jQuery(this);
		jQuery.ajax({
			type : "get",
			url : "supersede-alerts?refreshAlerts=y",
			success : function(data) {
				console.log('dom', self, data);
				$("#data").html(data);
				// self.parent().parent().remove();
				AJS.tablessortable.setTableSortable(AJS.$(".sortableTable"));
				onPageLoad();
			},
			error : function() {
				console.log('error', arguments);
			}
		});
	});

	jQuery('.searchBtn').click(
			function() {
				$("#selectionList").val(selectionString);
				var self = jQuery(this);
				var searchStr = $('#searchAlertsInput').val();
				if (!searchStr) {
					searchStr = "";
				}
				jQuery.ajax({
					type : "get",
					url : "supersede-alerts?searchAlerts=y&searchAlertsInput="
							+ searchStr,
					success : function(data) {
						console.log('dom', self, data);
						$("#data").html(data);
						// self.parent().parent().remove();
						AJS.tablessortable.setTableSortable(AJS
								.$(".sortableTable"));
						onPageLoad();
					},
					error : function() {
						console.log('error', arguments);
					}
				});
			});

	jQuery('.searchBtnDialog').click(
			function() {
				$("#selectionList").val(selectionString);
				var self = jQuery(this);
				var searchStr = $('#searchAlertsInput').val();
				var searchIssueStr = $('#searchIssuesDialogInput').val();
				if (!searchStr) {
					searchStr = " ";
				}
				if (!searchIssueStr) {
					searchIssueStr = " ";
				}
				jQuery.ajax({
					type : "get",
					url : "supersede-alerts?searchIssues=y&searchAlertsInput="
							+ searchStr + "&searchIssuesInput="
							+ searchIssueStr,
					success : function(data) {
						console.log('dom', self, data);
						$("#attach-dialog-data").html(data);
						AJS.tablessortable.setTableSortable(AJS
								.$(".sortableDialogTable"));
						onPageLoad();
					},
					error : function() {
						console.log('error', arguments);
					}
				});
			});

	jQuery("#searchAlertsInput").keyup(function(event) {
		if (event.keyCode == 13) {
			$(".searchBtn").click();
		}
	});

	jQuery('.chkSelected').click(
			function() {
				if ($(this).prop('checked')) {
					// Write a string containing the IDs of selected
					// alerts
					selectionString += $(this).attr('id');
					selectionString += ':::';
					//alert(selectionString);
					$(".toEnable").prop('disabled', false);
					$(".toEnable").prop('enabled', true);
					// alert(selectionString);
				} else {
					// Remove the selected ID if checkbox gets unchecked
					selectionString = selectionString.replace($(this)
							.attr('id')
							+ ':::', '');
					if (!selectionString) {
						$(".toEnable").prop('disabled', true);
						$(".toEnable").prop('enabled', false);
					}
				}
			});
	jQuery('.dialogChkSelected').click(
			function() {
				if ($(this).prop('checked')) {
					// Write a string containing the IDs of selected
					// alerts
					// alert("entered");
					issuesSelectionString += $(this).attr('id');
					issuesSelectionString += ':::';
					$(".toEnableDialog").prop('disabled', false);
					$(".toEnableDialog").prop('enabled', true);
				} else {
					// Remove the selected ID if checkbox gets unchecked
					issuesSelectionString = issuesSelectionString.replace($(
							this).attr('id')
							+ ':::', '');
					if (!issuesSelectionString) {
						$(".toEnableDialog").prop('disabled', true);
						$(".toEnableDialog").prop('enabled', false);
					}
				}
			});

	var opt = {
		autoOpen : false,
		modal : true,
		width : 550,
		height : 650,
		title : 'Details'
	};

	jQuery('.opener').click(function() {
		$("#dialog-1").dialog(opt).dialog("open");
	});

	jQuery('.alertManagement').click(function() {
		console.log('alert management');
		$(".selectionList").val(selectionString);
	});

	// Shows the dialog when the "Show dialog" button is clicked
	AJS.$("#attach-dialog-show-button").click(function() {
		AJS.dialog2("#attach-dialog").show();
	});

	AJS.$("#import-dialog-show-button").click(function() {
		AJS.dialog2("#import-dialog").show();
	});

	AJS.$("#check-similarity").click(function() {
		AJS.dialog2("#similarity-dialog").show();
	});

	AJS.$("#bulk-dialog-show-button").click(function() {
		AJS.dialog2("#bulk-dialog").show();
	});
	
	AJS.$("#clusterization-dialog-show-button").click(function() {
		AJS.dialog2("#clusterization-dialog").show();
	});

	AJS
			.$("#dialog-delete-button")
			.click(
					function() {
						var alerts = selectionString;
						var splitAlerts = alerts.split(':::');
						var deleteList = '<ul>';
						for (var i = 0; i < splitAlerts.length - 1; i++) {
							deleteList += '<li><a href="/jira/plugins/servlet/supersede-alerts?xmlAlert=y&xmlAlertId='
									+ splitAlerts[i]
									+ '" target="_blank">'
									+ splitAlerts[i] + '</a><br/></li>';
						}
						deleteList += '</ul>';
						$('.listToDelete').html(deleteList);
						AJS.dialog2("#delete-dialog").show();
					});

	// Hides the dialog
	AJS.$("#dialog-close-button").click(function(e) {
		e.preventDefault();
		AJS.dialog2("#attach-dialog").hide();
	});

	// DIALOG
	jQuery('.dialogButton').click(function() {
		console.log('alert management');
		$(".issuesSelectionList").val(issuesSelectionString);
		$("#issuesSelectionList").val(issuesSelectionString);
		$("#selectionList").val(selectionString);
		$(".selectionList").val(selectionString);
		
	});

	// DIALOG
	jQuery('.dialogDeleteButton').click(function() {
		console.log('alert management')
		$("#selectionList").val(selectionString);
	});

	jQuery('.projectElement').click(function() {
		$(".projectElement").removeClass('aui-nav-selected');
		$(".projectElement").addClass('aui-nav');
		$(this).removeClass('aui-nav').addClass('aui-nav-selected');
	});

	AJS.$(".simple-tooltip").tooltip();

	$('#project-select-import').change(
			function() {
				$('.projectField').val($(this).val());
				var self = jQuery(this);
				jQuery.ajax({
					type : "get",
					url : "supersede-alerts?getIssueTypes=y&projectField="
							+ $(this).val(),
					success : function(data) {
						$("#issue-type").html(data);
						AJS.$('#issue-type-selector').auiSelect2();
						$('#issue-type-selector').on('change',
								issueTypeSelectorChange);
					},
					error : function() {
						console.log('error', arguments);
					}
				});
			});
	
	$('#project-select-bulk').change(
			function() {
				$('.projectField').val($(this).val());
				var self = jQuery(this);
				jQuery.ajax({
					type : "get",
					url : "supersede-alerts?getIssueTypes=y&projectField="
							+ $(this).val(),
					success : function(data) {
						$("#issue-type").html(data);
						AJS.$('#issue-type-selector').auiSelect2();
						$('#issue-type-selector').on('change',
								issueTypeSelectorChange);
					},
					error : function() {
						console.log('error', arguments);
					}
				});
			});

	$('#project-select-attach').change(
			function() {
				$('.projectField').val($(this).val());
				var self = jQuery(this);
				jQuery.ajax({
					type : "get",
					url : "supersede-alerts?searchIssues=y&projectField="
							+ $(this).val(),
					success : function(data) {
						console.log('dom', self, data);
						$("#attach-dialog-data").html(data);
						// self.parent().parent().remove();
						AJS.tablessortable.setTableSortable(AJS
								.$(".sortableDialogTable"));
						onPageLoad();
					},
					error : function() {
						console.log('error', arguments);
					}
				});
				// alert("load!");
			});

	AJS.$("#similarity-button").click(function() {
		$('.issueFilter').val($('#search-similarity-filters').val());
		$('.similarity-number').val($('#select-similarity-number').val());

	});
	
	AJS.$("#clusterization-button").click(function() {
		$('.clusterization-tolerance').val($('#select-clusterization-tolerance').val());
		$('.clusterization-number').val($('#select-clusterization-number').val());

	});

	function issueTypeSelectorChange() {
		$('.issueType').val($(this).val());
	}
	;
}
