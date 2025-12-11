const fs = require("fs");
const fetch = global.fetch;

async function getModrinthDownloads(projectId) {
  const res = await fetch(`https://api.modrinth.com/v2/project/${projectId}`);
  if (!res.ok) return 0;
  const data = await res.json();
  return data.downloads ?? 0;
}

async function getGithubDownloads(owner, repo) {
  const res = await fetch(`https://api.github.com/repos/${owner}/${repo}/releases`);
  if (!res.ok) return 0;
  const releases = await res.json();
  return releases.reduce((sum, rel) => sum + rel.assets.reduce((a, asset) => a + asset.download_count, 0), 0);
}

(async () => {
  const modrinth = await getModrinthDownloads("lWNvJAlY");
  const github = await getGithubDownloads("deutschich", "BalSync");
  const total = modrinth + github;

  // JSON für Shields.io
  const badgeJson = {
    schemaVersion: 1,
    label: "downloads",
    message: total.toString(),
    color: "blue"
  };

  fs.writeFileSync("badges/downloads.json", JSON.stringify(badgeJson, null, 2));
})();
