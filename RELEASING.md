* Add CHANGELOG.md to what changes for new version
* Delete -SNAPSHOT from plugin.xml version tag
* Add plugin.xml change note
```xml
    <change-notes><![CDATA[
        <p>1.0.1</p>
        <ul>
            <li>Fix issue #2 invalid revision range</li>
            <li>Fix issue #5 show error/info notifications</li>
        </ul>

        <p>1.0.0</p>
        <ul>
            <li>Initial release</li>
        </ul>
    ]]>
    </change-notes>
```
* Build > Prepare Plugin Module 'plugin-name' For Deployment
* Commit & push changes
* Create Release Tag (Upload archive file as well)
* Upload to https://plugins.jetbrains.com/
* Prepare for next version (-> Increment plugin.xml's version and add `-SNAPSHOT`)
