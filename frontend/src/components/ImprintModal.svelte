<script>
  import { createEventDispatcher } from 'svelte';

  const dispatch = createEventDispatcher();

  export function close() {
    dispatch('close');
  }
</script>

<div
  id="imprint-modal"
  role="dialog"
  aria-labelledby="imprint-modal-title"
  aria-modal="true"
  class="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4 overflow-y-auto"
>
  <div class="bg-white rounded-xl border border-[#e0e3e5] max-w-lg w-full p-6 shadow-2xl my-8 relative text-[#191c1e] font-sans">
    <div class="flex items-center justify-between pb-4 border-b border-[#e0e3e5] mb-4">
      <h2 id="imprint-modal-title" class="text-xl font-bold text-[#003f87]">
        Выходные данные (Imprint / Impressum)
      </h2>
      <button
        type="button"
        on:click={close}
        aria-label="Закрыть выходные данные"
        class="text-[#727784] hover:text-[#191c1e] text-2xl font-bold p-1 rounded-full focus:outline-none focus:ring-2 focus:ring-[#003f87]/50"
      >
        ✕
      </button>
    </div>

    <div class="space-y-4 text-sm text-[#424752] leading-relaxed">
      <section>
        <h3 class="font-bold text-[#191c1e] text-base mb-1">Наименование организации</h3>
        <p>ФБУН «Российский научно-исследовательский институт эпидемиологии»</p>
      </section>

      <section>
        <h3 class="font-bold text-[#191c1e] text-base mb-1">Юридический и фактический адрес</h3>
        <p>111123, г. Москва, ул. Novogireevskaya, д. 3А</p>
      </section>

      <section>
        <h3 class="font-bold text-[#191c1e] text-base mb-1">Контакты</h3>
        <p>Телефон: +7 (495) 123-45-67</p>
        <p>Электронная почта: <a href="mailto:info@epidemiology-inst.ru" class="text-[#003f87] underline hover:text-[#002b5e]">info@epidemiology-inst.ru</a></p>
      </section>

      <section>
        <h3 class="font-bold text-[#191c1e] text-base mb-1">Ответственный за обработку персональных данных</h3>
        <p>Отдел защиты информации и конфиденциальности (152-ФЗ)</p>
        <p>Email: <a href="mailto:privacy@epidemiology-inst.ru" class="text-[#003f87] underline hover:text-[#002b5e]">privacy@epidemiology-inst.ru</a></p>
      </section>

      <section class="border-t border-[#e0e3e5] pt-3 text-xs text-[#727784]">
        <p>Правовое уведомление: Информация на данном портале предназначена исключительно для служебного и научного использования медицинскими специалистами и сотрудниками института.</p>
      </section>
    </div>

    <div class="mt-6 flex justify-end border-t border-[#e0e3e5] pt-4">
      <button
        type="button"
        on:click={close}
        class="px-5 py-2.5 bg-[#003f87] hover:bg-[#002b5e] text-white font-medium text-xs rounded-lg transition-colors focus:outline-none focus:ring-2 focus:ring-[#003f87]/50"
      >
        Закрыть
      </button>
    </div>
  </div>
</div>
