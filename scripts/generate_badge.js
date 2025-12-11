import fetch from "node-fetch";
import fs from "fs";

// ---- Plattformen abrufen ----

async function getModrinthDownloads(projectId) {
  const r = await fetch(`https://api.modrinth.com/v2/project/${projectId}`);
  const j = await r.json();
  return j.downloads ?? 0;
}

async function getGithubDownloads(owner, repo) {
  const r = await fetch(`https://api.github.com/repos/${owner}/${repo}/releases`);
  const releases = await r.json();
  return releases.reduce((sum, rel) => {
    return sum + rel.assets.reduce((a, asset) => a + asset.download_count, 0);
  }, 0);
}

// ---- Badge erstellen ----

function createBadge(total) {
  return `
<svg xmlns="http://www.w3.org/2000/svg" width="150" height="20">
  <rect width="70" height="20" fill="#555"/>
  <rect x="70" width="80" height="20" fill="#007ec6"/>
  <text x="35" y="14" fill="#fff" font-family="Verdana" font-size="11" text-anchor="middle">downloads</text>
  <text x="110" y="14" fill="#fff" font-family="Verdana" font-size="11" text-anchor="middle">${total}</text>
</svg>`;
}

// ---- Gesamtdownloads berechnen ----

(async () => {
  const modrinth = await getModrinthDownloads("DEIN_MODRINTH_ID");
  const github = await getGithubDownloads("DEINUSER", "DEINREPO");

  const total = modrinth + github;

  const badge = createBadge(total);

  fs.writeFileSync("badges/downloads.svg", badge);
})();
