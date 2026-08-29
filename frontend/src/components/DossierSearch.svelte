<script>
    let surname = "";
    let selectedPeriod = "ALL";
    let selectedDirection = "ALL";
    let selectedDocType = "ALL";

    let documents = [];
    let isSearched = false;
    let loading = false;
    let feedback = "";

    const allDocuments = [
        { id: 1, title: "Приказ о назначении №42", type: "order", direction: "surveillance", period: "2023", author: "Петров П.П.", docTypeName: "Приказы", directionName: "Эпиднадзор" },
        { id: 2, title: "Выписка из учёного совета от 12.05.2023", type: "extract", direction: "virology", period: "2023", author: "Иванов И.И.", docTypeName: "Выписки из учёного совета", directionName: "Вирусология" },
        { id: 3, title: "Отчёт о командировке (Самара)", type: "report", direction: "bacteriology", period: "2023", author: "Иванов И.И.", docTypeName: "Отчёты", directionName: "Бактериология" },
        { id: 4, title: "Научная публикация: Мониторинг вирусов гриппа A/H1N1", type: "publication", direction: "virology", period: "2024", author: "Петров П.П.", docTypeName: "Научные публикации", directionName: "Вирусология" },
        { id: 5, title: "Аналитический отчет по вакцинопрофилактике кори", type: "report", direction: "vaccination", period: "2024", author: "Иванов И.И.", docTypeName: "Отчёты", directionName: "Вакцинопрофилактика" },
        { id: 6, title: "Научная публикация: Генотипирование бактериальных штаммов", type: "publication", direction: "bacteriology", period: "2023", author: "Сидоров С.С.", docTypeName: "Научные публикации", directionName: "Бактериология" },
        { id: 7, title: "Приказ об организации эпиднадзора №105", type: "order", direction: "surveillance", period: "2024", author: "Петров П.П.", docTypeName: "Приказы", directionName: "Эпиднадзор" }
    ];

    function searchDossier() {
        isSearched = true;
        applyFilters();
    }

    function applyFilters() {
        let result = allDocuments;

        if (surname.trim()) {
            const query = surname.trim().toLowerCase();
            result = result.filter(doc => doc.author.toLowerCase().includes(query));
        }

        if (selectedPeriod !== "ALL") {
            result = result.filter(doc => doc.period === selectedPeriod);
        }

        if (selectedDirection !== "ALL") {
            result = result.filter(doc => doc.direction === selectedDirection);
        }

        if (selectedDocType !== "ALL") {
            result = result.filter(doc => doc.type === selectedDocType);
        }

        documents = result;
    }

    function handleFilterChange() {
        if (isSearched) {
            applyFilters();
        }
    }

    function generateReport() {
        loading = true;
        feedback = "";
        setTimeout(() => {
            loading = false;
            feedback = "✓ Сводный PDF-документ успешно сформирован и скачан.";
            downloadPdfReport();
        }, 800);
    }

    function downloadPdfReport() {
        const dummyPdfContent = `%PDF-1.4\n1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R >>\nendobj\n4 0 obj\n<< /Length 55 >>\nstream\nBT\n/F1 12 Tf\n100 700 Td\n(Analytical Dossier Summary Report) Tj\nET\nendstream\nendobj\nxref\n0 5\n0000000000 65535 f \n0000000009 00000 n \n0000000058 00000 n \n0000000115 00000 n \n0000000216 00000 n \ntrailer\n<< /Size 5 /Root 1 0 R >>\nstartxref\n321\n%%EOF`;
        const blob = new Blob([dummyPdfContent], { type: 'application/pdf' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `dossier_report_${surname.trim() || 'summary'}.pdf`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
    }
</script>

<div class="dossier-container bg-white rounded-xl border border-[#e0e3e5] p-6 shadow-sm space-y-6">
    <header class="border-b border-[#e0e3e5] pb-4">
        <h2 class="text-xl font-bold text-[#003f87]">Формирование досье и аналитических справок</h2>
        <p class="text-xs text-[#424752] mt-1">Гранулярная фильтрация документов по сотрудникам, периодам и научным направлениям</p>
    </header>

    <div class="filters-grid grid grid-cols-1 md:grid-cols-12 gap-4 bg-[#f7f9fb] p-4 rounded-lg border border-[#e0e3e5]">
        <div class="md:col-span-4">
            <label for="search-query-input" class="block text-xs font-semibold text-[#191c1e] mb-1">
                Фамилия сотрудника
            </label>
            <input
                type="text"
                bind:value={surname}
                on:keydown={(e) => e.key === 'Enter' && searchDossier()}
                placeholder="Фамилия сотрудника"
                id="search-query-input"
                class="w-full h-11 px-3.5 bg-white border border-[#c2c6d4] rounded-lg text-sm text-[#191c1e] focus:outline-none focus:ring-2 focus:ring-[#003f87]"
            />
        </div>

        <div class="md:col-span-3">
            <label for="filter-period" class="block text-xs font-semibold text-[#191c1e] mb-1">
                Период
            </label>
            <select
                id="filter-period"
                bind:value={selectedPeriod}
                on:change={handleFilterChange}
                class="w-full h-11 px-3 bg-white border border-[#c2c6d4] rounded-lg text-sm text-[#191c1e] focus:outline-none focus:ring-2 focus:ring-[#003f87]"
            >
                <option value="ALL">Все периоды</option>
                <option value="2024">2024 год</option>
                <option value="2023">2023 год</option>
            </select>
        </div>

        <div class="md:col-span-3">
            <label for="filter-direction" class="block text-xs font-semibold text-[#191c1e] mb-1">
                Научное направление
            </label>
            <select
                id="filter-direction"
                bind:value={selectedDirection}
                on:change={handleFilterChange}
                class="w-full h-11 px-3 bg-white border border-[#c2c6d4] rounded-lg text-sm text-[#191c1e] focus:outline-none focus:ring-2 focus:ring-[#003f87]"
            >
                <option value="ALL">Все направления</option>
                <option value="virology">Вирусология</option>
                <option value="bacteriology">Бактериология</option>
                <option value="surveillance">Эпиднадзор</option>
                <option value="vaccination">Вакцинопрофилактика</option>
            </select>
        </div>

        <div class="md:col-span-2 flex items-end">
            <button
                on:click={searchDossier}
                id="search-button"
                class="w-full min-h-[44px] min-w-[44px] bg-[#003f87] text-white font-bold text-xs rounded-lg hover:bg-[#002b5e] focus:outline-none focus:ring-2 focus:ring-[#003f87] transition-colors shadow-sm"
                style="color: #ffffff !important; background-color: #003f87 !important;"
            >
                Поиск
            </button>
        </div>
    </div>

    <div class="secondary-filters flex flex-wrap items-center gap-3">
        <label for="filter-doc-type" class="text-xs font-semibold text-[#191c1e]">Тип документа:</label>
        <select
            id="filter-doc-type"
            bind:value={selectedDocType}
            on:change={handleFilterChange}
            class="h-9 px-3 bg-white border border-[#c2c6d4] rounded-md text-xs text-[#191c1e] focus:outline-none focus:ring-2 focus:ring-[#003f87]"
        >
            <option value="ALL">Все типы документов</option>
            <option value="publication">Научные публикации</option>
            <option value="order">Приказы</option>
            <option value="extract">Выписки из учёного совета</option>
            <option value="report">Отчёты</option>
        </select>
    </div>

    {#if isSearched}
        {#if documents.length > 0}
            <div id="results-container" class="space-y-4">
                <div class="flex items-center justify-between">
                    <h3 class="text-base font-bold text-[#191c1e]">
                        Состав документов досье <span class="text-xs font-normal text-[#424752]">({documents.length})</span>
                    </h3>
                </div>

                <ul class="document-list space-y-2" id="document-list">
                    {#each documents as doc}
                        <li class="p-3 bg-[#f7f9fb] border border-[#e0e3e5] rounded-lg flex flex-col sm:flex-row sm:items-center justify-between gap-2 text-sm">
                            <div class="flex items-center gap-2">
                                <span class="text-base">📄</span>
                                <span class="font-semibold text-[#191c1e]">{doc.title}</span>
                            </div>
                            <div class="flex items-center gap-2 text-xs text-[#424752]">
                                <span class="px-2 py-0.5 rounded-full bg-[#d9e3f1] text-[#001a40] font-medium">{doc.directionName}</span>
                                <span class="px-2 py-0.5 rounded bg-[#eceef0] text-[#191c1e]">{doc.period} г.</span>
                                <span class="font-medium text-[#003f87]">{doc.author}</span>
                            </div>
                        </li>
                    {/each}
                </ul>

                <div class="report-section pt-4 border-t border-[#e0e3e5] flex items-center justify-between">
                    <button
                        on:click={generateReport}
                        id="generate-report-button"
                        disabled={loading}
                        class="min-h-[44px] px-6 py-2.5 bg-[#003f87] text-white text-xs font-bold rounded-lg hover:bg-[#002b5e] focus:outline-none focus:ring-2 focus:ring-[#003f87] disabled:bg-[#a0c4ff] transition-colors flex items-center justify-center gap-2 shadow-sm"
                        style="color: #ffffff !important; background-color: #003f87 !important;"
                    >
                        {#if loading}
                            <span id="loading-spinner" class="flex items-center gap-2">
                                <svg class="animate-spin h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                                </svg>
                                Генерация PDF-досье...
                            </span>
                        {:else}
                            <span>📥 Сформировать и экспортировать PDF</span>
                        {/if}
                    </button>
                </div>

                {#if feedback}
                    <div role="status" aria-live="polite" class="feedback-notice p-3 bg-[#d9e3f1] text-[#001a40] rounded-lg font-medium text-xs border border-[#003f87] flex items-center gap-2">
                        {feedback}
                    </div>
                {/if}
            </div>
        {:else}
            <div id="results-container" class="p-8 text-center bg-[#f7f9fb] border border-[#e0e3e5] rounded-lg">
                <p class="text-sm font-semibold text-[#424752]">нет материалов по выбранным критериям</p>
                <p class="text-xs text-[#727784] mt-1">Попробуйте изменить параметры фильтрации или ввести другую фамилию сотрудника.</p>
            </div>
        {/if}
    {/if}

    <footer class="dossier-footer pt-4 border-t border-[#e0e3e5] text-xs text-[#727784] flex flex-col sm:flex-row justify-between items-center gap-2">
        <span>Российский научно-исследовательский институт эпидемиологии</span>
        <button on:click={() => alert('Выходные данные (Imprint / Impressum):\nФБУН «НИИ Эпидемиологии»\nг. Москва, ул. Новогиреевская, 3А')} class="imprint-btn text-[#003f87] underline hover:text-[#002b5e] focus:outline-none focus:ring-2 focus:ring-[#003f87] rounded px-1">
            Выходные данные (Imprint / Impressum)
        </button>
    </footer>
</div>
