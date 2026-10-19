import net.uaznia.lukanus.hudson.plugins.gitparameter.GitParameterDefinition
import net.uaznia.lukanus.hudson.plugins.gitparameter.SortMode
import net.uaznia.lukanus.hudson.plugins.gitparameter.SelectedValue
def out = []
try {
    out << 'sm class=' + SortMode.DESCENDING_SMART.getClass().name
    out << 'SelectedValue statics:'
    ['NONE', 'TOP', 'SELECTED', 'DEFAULT_VALUE', 'INDEX'].each { n ->
        try {
            def v = SelectedValue."$n"
            out << ('  ' + n + ' = ' + v)
        } catch (e) {
            out << ('  ' + n + ' missing')
        }
    }
    def p = new GitParameterDefinition('BRANCH', 'PT_BRANCH', 'origin/main', '选择要构建的分支或标签', '*', '.*', '*', SortMode.DESCENDING_SMART, SelectedValue.NONE, 'https://github.com/yinshi197/JenkinsFile_repo.git', false)
    out << Jenkins.XSTREAM2.toXML(p)
} catch (e) {
    out << ('FAILED: ' + e.toString())
}
return out.join('\n---\n')
