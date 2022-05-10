import React from "react";
import { JENKINS_PIPELINE_CONFIG } from "./jenkinsConfig";
import { BAMBOO_PIPELINE_CONFIG } from "./bambooConfig";
import { GITHUB_ACTIONS_CONFIG } from "./githubActionsConfig";
import { GHE_ACTIONS_CONFIG } from "./gheActionsConfig";
import { BAMBOO_DEPLOYED_PIPELINE_CONFIG } from "./bambooDeployedConfig";
import { PipelineTool } from "../../models/pipeline";

export const PIPELINE_CONFIG = {
	[PipelineTool.JENKINS]: JENKINS_PIPELINE_CONFIG,
	[PipelineTool.BAMBOO]: BAMBOO_PIPELINE_CONFIG,
	[PipelineTool.GITHUB_ACTIONS]: GITHUB_ACTIONS_CONFIG,
	[PipelineTool.GHE_ACTIONS]: GHE_ACTIONS_CONFIG,
	[PipelineTool.BAMBOO_DEPLOYMENT]: BAMBOO_DEPLOYED_PIPELINE_CONFIG,
};

const githubActionNote = (
	<div css={{ color: "rgba(0,0,0,0.25)", whiteSpace: "normal", marginBottom: 50 }}>
		Note: Deployment data is collected from workflow execution history. All you have to provide here
		is the URL of your Github Repository and we can find all associated workflow executions for you
		automatically. Struggle with the terms? More details please refer to:{" "}
		<a
			href={"https://github.com/features/actions"}
			css={{
				textDecoration: "underline",
				"&:hover": { textDecoration: "underline" },
			}}>
			https://github.com/features/actions
		</a>
	</div>
);

export const PIPELINE_TYPE_NOTE = {
	[PipelineTool.JENKINS]: undefined,
	[PipelineTool.BAMBOO]: (
		<div css={{ color: "rgba(0,0,0,0.25)", whiteSpace: "normal", marginBottom: 50 }}>
			Note: Deployment data is ought to be collected from Bamboo &quot;Build Plans&quot; and/or
			&quot;Deployment Projects&quot;. All you have to provide here is the URL of your &quot;Build
			Plan&quot; and the tool can find all associated deployment projects for you automatically.
			Struggle with the terms? More details please refer to:{" "}
			<a
				href={"https://confluence.atlassian.com/bamboo0700/deployment-projects-1014682237.html"}
				css={{
					textDecoration: "underline",
					"&:hover": { textDecoration: "underline" },
				}}>
				https://confluence.atlassian.com/bamboo0700/deployment-projects-1014682237.html
			</a>
		</div>
	),
	[PipelineTool.GITHUB_ACTIONS]: githubActionNote,
	[PipelineTool.GHE_ACTIONS]: githubActionNote,
	[PipelineTool.BAMBOO_DEPLOYMENT]: (
		<div css={{ color: "rgba(0,0,0,0.25)", whiteSpace: "normal", marginBottom: 50 }}>
			Note: Deployment data is ought to be collected from Bamboo &quot;Build Plans&quot; and/or
			&quot;Deployment Projects&quot;. All you have to provide here is a URL contains &quot;Bamboo
			Deployment ID&quot; and the tool can find all associated deployment projects for you
			automatically. Struggle with the terms? More details please refer to:{" "}
			<a
				href={"https://confluence.atlassian.com/bamboo0700/deployment-projects-1014682237.html"}
				css={{
					textDecoration: "underline",
					"&:hover": { textDecoration: "underline" },
				}}>
				https://confluence.atlassian.com/bamboo0700/deployment-projects-1014682237.html
			</a>
		</div>
	),
};
