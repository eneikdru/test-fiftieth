<script>
  import { createEventDispatcher } from 'svelte';

  export let document = null;
  export let apiBaseUrl = '';

  const dispatch = createEventDispatcher();

  let hasError = false;

  function handleClose() {
    dispatch('close');
  }

  function handleDownload() {
    dispatch('download', { document });
  }

  function handlePrint() {
    if (document && document.id) {
      const iframe = window.document.getElementById('doc-viewer-iframe');
      if (iframe && iframe.contentWindow) {
        try {
          iframe.contentWindow.print();
        } catch (e) {
          console.error('Print failed:', e);
          window.open(`${apiBaseUrl}/documents/${document.id}/view`, '_blank').print();
        }
      }
    }
  }

  function onIframeError() {
    hasError = true;
  }

  function getViewerUrl(doc) {
    if (doc && doc.id) {
      return `${apiBaseUrl}/documents/${doc.id}/view`;
    }
    return '';
  }

</script>

<div class="fixed inset-0 z-50 bg-on-surface/20 backdrop-blur-md flex flex-col pt-16">
  <!-- TopAppBar (Floating above document) -->
  <header class="absolute top-0 w-full flex items-center justify-between px-4 sm:px-6 lg:px-8 h-16 bg-surface dark:bg-surface-dim border-b border-outline-variant dark:border-on-surface-variant shadow-sm z-30 transition-colors duration-200 ease-in-out">
    <button
      on:click={handleClose}
      class="text-on-surface-variant dark:text-outline hover:bg-surface-container dark:hover:bg-surface-container-highest p-2 rounded-full flex items-center justify-center transition-colors focus:ring-2 focus:ring-primary/50 focus:outline-none"
      aria-label="Back"
    >
      <span class="material-symbols-outlined" data-icon="arrow_back">arrow_back</span>
    </button>
    <h1 class="font-headline-md text-headline-md font-bold text-on-surface dark:text-on-surface-variant truncate max-w-lg text-center">
      {document ? (document.title || document.fileName || 'Document Preview') : 'Document Preview'}
    </h1>
    <button class="text-on-surface-variant dark:text-outline hover:bg-surface-container dark:hover:bg-surface-container-highest p-2 rounded-full flex items-center justify-center transition-colors focus:ring-2 focus:ring-primary/50 focus:outline-none" aria-label="Search">
      <span class="material-symbols-outlined" data-icon="search">search</span>
    </button>
  </header>

  <!-- Document Preview Canvas -->
  <main class="flex-1 bg-surface-container-lowest m-4 rounded-xl shadow-lg border border-outline-variant overflow-y-auto relative z-20 flex flex-col mb-24">
    {#if document}
      {#if hasError || (document.fileName && !document.fileName.toLowerCase().endsWith('.pdf') && !document.fileName.toLowerCase().endsWith('.txt'))}
        <div class="flex flex-col items-center justify-center h-full p-8 text-center space-y-4">
           <span class="material-symbols-outlined text-4xl text-error" data-icon="error">error</span>
           <h2 class="font-headline-lg text-headline-lg text-on-surface">Preview Not Available</h2>
           <p class="font-body-md text-body-md text-on-surface-variant">The document format is unsupported for inline viewing, or an error occurred.</p>
           <button
              on:click={handleDownload}
              class="mt-4 py-2 px-6 bg-primary hover:opacity-90 focus:ring-2 focus:ring-primary/50 focus:outline-none text-on-primary text-sm font-medium rounded-md transition-colors flex items-center justify-center gap-2 shadow-sm"
           >
             <span class="material-symbols-outlined text-[20px]" data-icon="download">download</span>
             <span>Download File Instead</span>
           </button>
        </div>
      {:else}
        <iframe
          id="doc-viewer-iframe"
          src={getViewerUrl(document)}
          title="Document Viewer"
          class="w-full h-full border-0 min-h-[800px]"
          on:error={onIframeError}
        ></iframe>
      {/if}
    {:else}
      <div class="flex items-center justify-center h-full">
         <p class="font-body-md text-body-md text-on-surface-variant">No document selected</p>
      </div>
    {/if}
  </main>

  <!-- Floating Controls Panel -->
  <div class="absolute bottom-6 left-1/2 transform -translate-x-1/2 bg-surface dark:bg-surface-dim rounded-full shadow-lg border border-outline-variant px-6 py-3 flex items-center gap-6 z-40">
    <button
      on:click={handleDownload}
      class="flex flex-col items-center justify-center gap-1 text-primary hover:opacity-80 transition-opacity focus:outline-none"
    >
      <span class="material-symbols-outlined text-[24px]" data-icon="download">download</span>
      <span class="font-label-caps text-label-caps text-[10px] uppercase font-bold tracking-wider">Download</span>
    </button>
    <div class="w-[1px] h-8 bg-outline-variant"></div>
    <button
      on:click={handlePrint}
      class="flex flex-col items-center justify-center gap-1 text-on-surface-variant hover:opacity-80 transition-opacity focus:outline-none"
    >
      <span class="material-symbols-outlined text-[24px]" data-icon="print">print</span>
      <span class="font-label-caps text-label-caps text-[10px] uppercase font-bold tracking-wider">Print</span>
    </button>
    <div class="w-[1px] h-8 bg-outline-variant"></div>
    <button
      on:click={handleClose}
      class="flex flex-col items-center justify-center gap-1 text-on-surface-variant hover:opacity-80 transition-opacity focus:outline-none"
    >
      <span class="material-symbols-outlined text-[24px]" data-icon="close">close</span>
      <span class="font-label-caps text-label-caps text-[10px] uppercase font-bold tracking-wider">Close</span>
    </button>
  </div>
</div>

<style>
  .backdrop-blur-md {
    backdrop-filter: blur(12px);
  }
</style>
