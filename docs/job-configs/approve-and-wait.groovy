def sa = org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval.get()
def out = []
def waited = 0
while (sa.getPendingScripts().size() == 0 && waited < 60000) {
    Thread.sleep(1000)
    waited += 1000
}
out << ('waitedMs=' + waited + ' pending=' + sa.getPendingScripts().size())
sa.getPendingScripts().each { p ->
    out << ('approving hash=' + p.hash)
    sa.doApproveScriptHash(p.hash)
}
out << ('pendingAfter=' + sa.getPendingScripts().size() + ' approvedCount=' + sa.getApprovedScriptHashes().size())
return out.join('\n')
