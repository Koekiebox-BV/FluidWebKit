# FluidWebKit
Fluid WebKit has a set of easy to use helper classes to quickly build custom front-ends using a JSF framework.

The required libraries for the kit are as follows;
<table>
    <tr><th>Software</th><th>Version</th></tr>
    <tr><td>JDK</td><td>v1.8</td></tr>
    <tr><td>JSF</td><td>v2.3</td></tr>
    <tr><td>PrimeFaces</td><td>v10.0.9</td></tr>
    <tr><td>Fluid-API</td><td>v1.12</td></tr>
    <tr><td>Fluid-WS</td><td>v1.12</td></tr>
    <tr><td>CDI</td><td>v1.1</td></tr>
</table>

## Workflow progress

In WebKit's form-type configuration, set **Show Workflow Progress** to **Yes** to
show a progress bar above the form fields. The setting defaults to **No** and is
stored as `showWorkflowProgress` in the WebKit form JSON. The host's shared form
body renders `wsFluidItem.showWorkflowProgress` and
`wsFluidItem.workflowProgressPercentage` (implemented by Rain's shared form body).

The bar is visible only for an existing item currently in workflow. Progress is
the completed introduction/submission milestone plus completed assignment steps,
divided by those completed milestones plus still-reachable assignment steps.
Introduction earns credit immediately on workflow entry and counts once per run. Automation connects the path but does not count as work. Conditional
outcomes remain possible until routing excludes them; rules are inspected without
executing them or predicting future field values. Each assignment counts once;
completed assignments retain credit even when a rework route remains possible.
Returning to an assignment makes that current step outstanding again. History
from earlier workflow runs is excluded. A workflow with introduction but no assignment steps reports 100%; a workflow
with neither introduction nor assignment milestones reports zero.

Pericard enables this for its nine workflow request form types and the
system-created TMK Under ZMK Import form. Other Pericard forms keep the default.
Existing installations receive these settings when the Pericard configuration
migration runs; the switch can also be changed in WebKit configuration.

The workspace **Progress %** column is configured separately from the form bar.
WebKit requests percentages with the view listing only when that column is enabled.
Pericard enables it for its workflow view groups. To hide the form bar, expand the
form type in **Workspace – Look & Feel → Form Definition Configuration**, set
**Show Workflow Progress** (the first setting) to **No**, then save. This does not
hide the workspace column.
