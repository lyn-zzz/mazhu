import fs from "node:fs";
import path from "node:path";
import process from "node:process";
import QRCode from "qrcode";

const root = process.cwd();
const siteDir = path.join(root, "site");
const assetsDir = path.join(siteDir, "assets");
const gradleFile = fs.readFileSync(path.join(root, "android/app/build.gradle.kts"), "utf8");
const changelog = fs.readFileSync(path.join(root, "CHANGELOG.md"), "utf8");

const versionName = matchRequired(gradleFile, /versionName\s*=\s*"([^"]+)"/, "versionName");
const versionCode = Number(matchRequired(gradleFile, /versionCode\s*=\s*(\d+)/, "versionCode"));
const repository = process.env.GITHUB_REPOSITORY || "lyn-zzz/mazhu";
const pageUrl = stripTrailingSlash(
  process.env.SITE_DOWNLOAD_PAGE_URL || `https://${repository.split("/")[0]}.github.io/${repository.split("/")[1]}/`,
) + "/";
const installPageUrl = new URL("download.html", pageUrl).toString();
const tagName = process.env.SITE_TAG_NAME || `v${versionName}`;
const apkUrl = process.env.SITE_APK_URL || `https://github.com/${repository}/releases/download/${tagName}/mazhu-${tagName}.apk`;
const sha256 = process.env.SITE_APK_SHA256 || "";
const releasedAt = process.env.SITE_RELEASE_DATE || new Date().toISOString().slice(0, 10);
const releaseNotes = extractReleaseNotes(changelog, versionName);

fs.mkdirSync(assetsDir, { recursive: true });

const latest = {
  versionName,
  versionCode,
  releasedAt,
  downloadPageUrl: pageUrl,
  installPageUrl,
  apkUrl,
  sha256,
  releaseNotes,
};

fs.writeFileSync(
  path.join(siteDir, "latest.json"),
  `${JSON.stringify(latest, null, 2)}\n`,
);

await QRCode.toFile(path.join(assetsDir, "download-qr.svg"), installPageUrl, {
  type: "svg",
  errorCorrectionLevel: "M",
  margin: 1,
  color: {
    dark: "#0f766e",
    light: "#ffffff",
  },
});

console.log(`Built site assets for ${versionName} (${versionCode})`);
console.log(`Download page: ${pageUrl}`);
console.log(`Install page: ${installPageUrl}`);

function matchRequired(text, pattern, label) {
  const match = text.match(pattern);
  if (!match) {
    throw new Error(`Could not find ${label}`);
  }
  return match[1];
}

function stripTrailingSlash(value) {
  return value.replace(/\/+$/, "");
}

function extractReleaseNotes(text, version) {
  const section = text.match(new RegExp(`##\\s+${escapeRegExp(version)}[^\\n]*\\n([\\s\\S]*?)(?=\\n##\\s+|$)`));
  if (!section) {
    return [];
  }
  return section[1]
    .split("\n")
    .map((line) => line.trim())
    .filter((line) => line.startsWith("- "))
    .map((line) => line.slice(2).trim())
    .filter(Boolean);
}

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}
