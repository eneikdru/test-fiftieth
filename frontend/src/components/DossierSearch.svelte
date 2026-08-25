<script>
    let surname = "";
    let documents = [];
    let loading = false;
    let feedback = "";

    function searchDossier() {
        if (!surname.trim()) return;
        documents = [
            { id: 1, title: "Приказ о назначении №42", type: "order" },
            { id: 2, title: "Выписка из учёного совета от 12.05.2023", type: "extract" },
            { id: 3, title: "Отчёт о командировке (Самара)", type: "report" }
        ];
    }

    function generateReport() {
        loading = true;
        feedback = "";
        setTimeout(() => {
            loading = false;
            feedback = "✓ Итоговая справка успешно сформирована.";
        }, 1000);
    }
</script>

<div class="dossier-container">
    <div class="search-section">
        <input
            type="text"
            bind:value={surname}
            placeholder="Фамилия сотрудника"
            id="search-query-input"
        />
        <button on:click={searchDossier} id="search-button">Поиск</button>
    </div>

    {#if documents.length > 0}
        <ul class="document-list" id="document-list">
            {#each documents as doc}
                <li>{doc.title}</li>
            {/each}
        </ul>
        <div class="report-section">
            <button on:click={generateReport} id="generate-report-button" disabled={loading}>
                {#if loading}
                    <span id="loading-spinner">Загрузка...</span>
                {:else}
                    Сформировать итоговую справку
                {/if}
            </button>
        </div>
        {#if feedback}
            <div role="status" aria-live="polite" class="feedback-notice">
                {feedback}
            </div>
        {/if}
    {/if}

    <footer class="dossier-footer">
        <span>Российский научно-исследовательский институт эпидемиологии</span>
        <button on:click={() => alert('Выходные данные (Imprint / Impressum):\nФБУН «НИИ Эпидемиологии»\nг. Москва, ул. Новогиреевская, 3А')} class="imprint-btn">
            Выходные данные (Imprint / Impressum)
        </button>
    </footer>
</div>

<style>
    .dossier-container { padding: 20px; font-family: sans-serif; }
    .search-section { margin-bottom: 20px; display: flex; gap: 10px; }
    input { padding: 8px; flex-grow: 1; border: 1px solid #ccc; border-radius: 4px; }
    button { padding: 8px 16px; background-color: #007bff; color: white; border: none; border-radius: 4px; cursor: pointer; }
    button:disabled { background-color: #a0c4ff; cursor: not-allowed; }
    .document-list { list-style: none; padding: 0; margin-bottom: 20px; }
    .document-list li { padding: 8px; border-bottom: 1px solid #eee; }
    .report-section { display: flex; align-items: center; }
    .feedback-notice { margin-top: 15px; padding: 10px; background-color: #d9e3f1; color: #001a40; border-radius: 4px; font-size: 14px; border: 1px solid #003f87; }
    .dossier-footer { margin-top: 40px; padding-top: 20px; border-top: 1px solid #eee; text-align: center; font-size: 12px; color: #666; display: flex; justify-content: space-between; align-items: center; }
    .imprint-btn { background: none; border: none; color: #007bff; text-decoration: underline; cursor: pointer; padding: 0; }
</style>
