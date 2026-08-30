const fs = require('fs');
const htmlFile = 'frontend/registration-harness.html';
let html = fs.readFileSync(htmlFile, 'utf8');

if (!html.includes('ImprintModal Stubbed')) {
  html = html.replace(
    'const source = await response.text();',
    `const sourceRaw = await response.text();
          const source = sourceRaw
              .replace("import ImprintModal from './ImprintModal.svelte';", "")
              .replace("<ImprintModal on:close={() => showImprint = false} />", "<!-- ImprintModal Stubbed -->");`
  );
  fs.writeFileSync(htmlFile, html);
}
