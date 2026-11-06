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
completed assignment steps divided by completed plus still-reachable assignment
steps. Automation connects the path but does not count as work. Conditional
outcomes remain possible until routing excludes them; rules are inspected without
executing them or predicting future field values. Each assignment counts once;
revisitable assignments remain outstanding, and history from earlier workflow
runs is excluded. A workflow with no assignment milestones reports zero.

Pericard enables this for its nine workflow request form types and the
system-created TMK Under ZMK Import form. Other Pericard forms keep the default.
Existing installations receive these settings when the Pericard configuration
migration runs; the switch can also be changed in WebKit configuration.
