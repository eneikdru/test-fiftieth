<script>
  import { createEventDispatcher, onMount } from 'svelte';

  export let document = null;
  export let apiBaseUrl = '';

  const dispatch = createEventDispatcher();

  let hasError = false;
  let viewerElement;

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

  function isUnsupportedFormat(doc) {
    if (!doc) return false;
    const path = doc.fileName || doc.filePath || doc.file_path || '';
    const lower = path.toLowerCase();
    if (lower.endsWith('.pdf') || lower.endsWith('.txt')) {
      return false;
    }
    return true;
  }

  function handleKeyDown(event) {
    if (event.key === 'Escape') {
      handleClose();
    }
  }

  onMount(() => {
    if (viewerElement) {
      viewerElement.focus();
    }
  });
</script>

<div
  bind:this={viewerElement}
  tabindex="-1"
  on:keydown={handleKeyDown}
  role="dialog"
  aria-modal="true"
  aria-labelledby="doc-viewer-title"
  id="doc-viewer-modal"
  class="fixed inset-0 z-50 bg-[#131b2e]/40 backdrop-blur-md flex flex-col pt-16 focus:outline-none"
>
  <!-- TopAppBar (Floating above document) -->
  <header class="absolute top-0 w-full flex items-center justify-between px-4 sm:px-6 lg:px-8 h-16 bg-[#faf8ff] dark:bg-[#131b2e] border-b border-[#c3c5d9] shadow-sm z-30 transition-colors duration-200 ease-in-out">
    <button
      type="button"
      on:click={handleClose}
      class="text-[#434656] dark:text-[#c3c5d9] hover:bg-[#eaedff] dark:hover:bg-[#283044] p-2 rounded-full flex items-center justify-center transition-colors focus:ring-2 focus:ring-[#003ec7]/50 focus:outline-none"
      aria-label="Закрыть просмотр"
      title="Закрыть"
    >
      <span class="material-symbols-outlined" data-icon="arrow_back">arrow_back</span>
    </button>
    <h1 id="doc-viewer-title" class="font-bold text-base sm:text-lg text-[#131b2e] dark:text-[#faf8ff] truncate max-w-md sm:max-w-xl text-center">
      {document ? (document.title || document.fileName || 'Просмотр документа') : 'Просмотр документа'}
    </h1>
    <button
      type="button"
      class="text-[#434656] dark:text-[#c3c5d9] hover:bg-[#eaedff] dark:hover:bg-[#283044] p-2 rounded-full flex items-center justify-center transition-colors focus:ring-2 focus:ring-[#003ec7]/50 focus:outline-none"
      aria-label="Поиск по документу"
      title="Поиск"
    >
      <span class="material-symbols-outlined" data-icon="search">search</span>
    </button>
  </header>

  <!-- Document Preview Canvas -->
  <main class="flex-1 bg-white m-2 sm:m-4 rounded-xl shadow-lg border border-[#c3c5d9] overflow-y-auto relative z-20 flex flex-col mb-24">
    {#if document}
      {#if hasError || isUnsupportedFormat(document)}
        <div id="doc-viewer-error" class="flex flex-col items-center justify-center h-full p-6 text-center space-y-4 my-auto">
           <div class="w-16 h-16 bg-[#ffdad6] rounded-full flex items-center justify-center text-[#93000a] text-2xl font-bold mb-2">
             ⚠️
           </div>
           <h2 class="text-xl font-bold text-[#131b2e]">Предпросмотр недоступен</h2>
           <p class="text-sm text-[#434656] max-w-md">
             Формат файла не поддерживается для встроенного просмотра или произошла ошибка при загрузке.
           </p>
           <button
              type="button"
              on:click={handleDownload}
              class="mt-4 py-2.5 px-6 bg-[#003f87] hover:bg-[#002b5e] focus:ring-2 focus:ring-[#003f87]/50 focus:outline-none text-white text-sm font-semibold rounded-lg transition-colors flex items-center justify-center gap-2 shadow-sm"
           >
             <span class="material-symbols-outlined text-[20px]" data-icon="download">download</span>
             <span>Скачать файл</span>
           </button>
        </div>
      {:else}
        <iframe
          id="doc-viewer-iframe"
          src={getViewerUrl(document)}
          title={document.title || "Документ"}
          class="w-full h-full border-0 min-h-[500px] sm:min-h-[800px]"
          on:error={onIframeError}
        ></iframe>
      {/if}
    {:else}
      <div class="flex items-center justify-center h-full p-8 text-center text-[#434656]">
         <p class="text-base font-medium">Документ не выбран</p>
      </div>
    {/if}
  </main>

  <!-- Floating Controls Panel -->
  <div class="fixed bottom-6 left-1/2 transform -translate-x-1/2 bg-[#faf8ff] dark:bg-[#131b2e] rounded-full shadow-xl border border-[#c3c5d9] px-5 py-2.5 flex items-center gap-4 sm:gap-6 z-40">
    <button
      type="button"
      on:click={handleDownload}
      class="flex flex-col items-center justify-center gap-1 text-[#003f87] hover:opacity-80 transition-opacity focus:outline-none focus:ring-2 focus:ring-[#003f87]/50 rounded-lg p-1"
      aria-label="Скачать документ"
    >
      <span class="material-symbols-outlined text-[24px]" data-icon="download">download</span>
      <span class="text-[10px] uppercase font-bold tracking-wider">Скачать</span>
    </button>
    <div class="w-[1px] h-8 bg-[#c3c5d9]"></div>
    <button
      type="button"
      on:click={handlePrint}
      class="flex flex-col items-center justify-center gap-1 text-[#434656] hover:opacity-80 transition-opacity focus:outline-none focus:ring-2 focus:ring-[#003f87]/50 rounded-lg p-1"
      aria-label="Печать документа"
    >
      <span class="material-symbols-outlined text-[24px]" data-icon="print">print</span>
      <span class="text-[10px] uppercase font-bold tracking-wider">Печать</span>
    </button>
    <div class="w-[1px] h-8 bg-[#c3c5d9]"></div>
    <button
      type="button"
      on:click={handleClose}
      class="flex flex-col items-center justify-center gap-1 text-[#434656] hover:opacity-80 transition-opacity focus:outline-none focus:ring-2 focus:ring-[#003f87]/50 rounded-lg p-1"
      aria-label="Закрыть окно просмотра"
    >
      <span class="material-symbols-outlined text-[24px]" data-icon="close">close</span>
      <span class="text-[10px] uppercase font-bold tracking-wider">Закрыть</span>
    </button>
  </div>
</div>

<style>
  .backdrop-blur-md {
    backdrop-filter: blur(12px);
  }
</style>
