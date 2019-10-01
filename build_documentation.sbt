import scala.sys.process._

val lastVersionString = "git tag -l".!!.split("\r?\n").last

version in Paradox := {
  if (isSnapshot.value)
    lastVersionString
  else version.value
}

paradoxProperties += ("app-version" -> {
  if (isSnapshot.value)
    lastVersionString
  else version.value
})

enablePlugins(ParadoxSitePlugin, ParadoxMaterialThemePlugin)

sourceDirectory in Paradox := sourceDirectory.value / "main" / "paradox"

enablePlugins(ParadoxMaterialThemePlugin)

Compile / paradoxMaterialTheme := {
    ParadoxMaterialTheme()
      .withLogoIcon("storage")
      .withCopyright("© QuadStingray 2018")
      .withColor("teal", "indigo")
      .withRepository(uri("https://github.com/QuadStingray/influx-logging"))
}

enablePlugins(SiteScaladocPlugin)

enablePlugins(GhpagesPlugin)

git.remoteRepo := "git@github.com:QuadStingray/influx-logging.git"

ghpagesNoJekyll := true