<script>
  export let onConsent = () => {};

  let showBanner = false;

  // Run initialization when component is created
  // Avoid using onMount to prevent module resolution issues in the simple test harness compiler
  if (typeof window !== 'undefined') {
    const consent = localStorage.getItem('consent_status');
    if (!consent) {
      showBanner = true;
    } else if (consent === 'rejected' || consent === 'accepted') {
       onConsent(consent);
    }
  }

  function handleAccept() {
    localStorage.setItem('consent_status', 'accepted');
    showBanner = false;
    onConsent('accepted');
  }

  function handleReject() {
    localStorage.setItem('consent_status', 'rejected');
    showBanner = false;
    onConsent('rejected');
  }
</script>

{#if showBanner}
  <div
    role="dialog"
    aria-labelledby="consent-banner-title"
    aria-describedby="consent-banner-desc"
    class="fixed bottom-0 left-0 w-full bg-[#f7f9fb] border-t-2 border-[#003f87] p-6 shadow-2xl z-50 flex flex-col md:flex-row items-center justify-between gap-4"
  >
    <div class="flex-1">
      <h2 id="consent-banner-title" class="text-lg font-bold text-[#003f87] mb-2">Настройки конфиденциальности</h2>
      <p id="consent-banner-desc" class="text-sm text-[#191c1e]">
        Мы используем файлы cookie для улучшения работы сайта. Вы можете принять их или отклонить использование необязательных трекеров.
      </p>
    </div>
    <div class="flex gap-3 shrink-0">
      <button
        on:click={handleReject}
        class="px-5 py-2.5 border-2 border-[#003f87] text-[#003f87] hover:bg-[#eceef0] focus:outline-none focus:ring-4 focus:ring-[#003f87]/50 text-sm font-semibold rounded-lg transition-colors"
        aria-label="Отклонить файлы cookie"
      >
        Отклонить
      </button>
      <button
        on:click={handleAccept}
        class="px-5 py-2.5 bg-[#003f87] text-white hover:bg-[#002b5e] focus:outline-none focus:ring-4 focus:ring-[#003f87]/50 text-sm font-semibold rounded-lg transition-colors shadow-md"
        aria-label="Принять файлы cookie"
      >
        Принять
      </button>
    </div>
  </div>
{/if}
