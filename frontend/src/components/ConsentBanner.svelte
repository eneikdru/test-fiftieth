<script>
  import { onMount } from 'svelte';

  let showBanner = false;

  onMount(() => {
    const consent = localStorage.getItem('cookie_consent');
    if (!consent) {
      showBanner = true;
    }
  });

  function accept() {
    localStorage.setItem('cookie_consent', 'accepted');
    showBanner = false;
  }

  function reject() {
    localStorage.setItem('cookie_consent', 'rejected');
    showBanner = false;
  }
</script>

{#if showBanner}
  <div class="fixed bottom-0 left-0 right-0 z-50 p-4 bg-white border-t border-[#e0e3e5] shadow-lg flex flex-col sm:flex-row items-center justify-between gap-4">
    <div class="text-sm text-[#191c1e]">
      Мы используем файлы cookie для улучшения работы сайта. Вы можете принять или отклонить использование необязательных файлов cookie.
    </div>
    <div class="flex items-center gap-3">
      <button
        class="min-h-[44px] min-w-[44px] px-4 py-2 bg-white text-[#424752] border border-[#c2c6d4] hover:bg-[#f7f9fb] rounded-md font-medium text-xs focus:outline-none focus:ring-2 focus:ring-[#003f87]"
        on:click={reject}
        aria-label="Отклонить"
      >
        Отклонить
      </button>
      <button
        class="min-h-[44px] min-w-[44px] px-4 py-2 bg-[#003f87] hover:bg-[#002b5e] text-white rounded-md font-medium text-xs focus:outline-none focus:ring-2 focus:ring-[#003f87]"
        on:click={accept}
        aria-label="Принять"
      >
        Принять
      </button>
    </div>
  </div>
{/if}
