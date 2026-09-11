const esbuild = require('esbuild');
const sveltePlugin = require('esbuild-svelte');

esbuild.build({
    entryPoints: {
      'RegistrationForm': 'src/components/RegistrationForm.svelte',
      'imprint-mount': 'src/imprint-mount.js'
    },
    bundle: true,
    outdir: 'dist',
    format: 'esm',
    plugins: [
        sveltePlugin({
            compilerOptions: { css: 'injected' }
        }),
    ],
}).catch((e) => { console.error(e); process.exit(1); });
