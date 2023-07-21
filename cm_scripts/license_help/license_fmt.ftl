<#function licenseFormat licenses>
    <#assign result = ""/>
    <#list licenses as license>
        <#assign result = result + license + ";"/>
    </#list>
    <#return result>
</#function>
<#function artifactFormat p>
    <#return p.groupId + ":" + p.artifactId + ":" + p.version + "=">
</#function>

<#if dependencyMap?size == 0>
The project has no dependencies.
<#else>
    <#list dependencyMap as e>
        <#assign project = e.getKey()/>
        <#assign licenses = e.getValue()/>
    ${artifactFormat(project)}${licenseFormat(licenses)} 
    </#list>
</#if>
