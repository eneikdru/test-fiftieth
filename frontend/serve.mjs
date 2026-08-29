import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const PORT = process.env.PORT || 18080;

const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.mjs': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.pdf': 'application/pdf',
};

const server = http.createServer((req, res) => {
  const parsedUrl = new URL(req.url, `http://localhost:${PORT}`);
  const pathname = parsedUrl.pathname;

  // Mock Dossier API endpoints
  if (pathname === '/api/v1/dossier/documents' && req.method === 'GET') {
    const employeeId = parsedUrl.searchParams.get('employee_id') || '';
    const query = parsedUrl.searchParams.get('query') || '';

    const docs = [
      {
        id: 1,
        employee_id: employeeId || "EMP-001",
        doc_type: "ORDER",
        title: "Приказ о назначении №42",
        doc_date: "2023-01-15",
        created_at: new Date().toISOString()
      },
      {
        id: 2,
        employee_id: employeeId || "EMP-001",
        doc_type: "EXTRACT",
        title: "Выписка из учёного совета от 12.05.2023",
        doc_date: "2023-05-12",
        created_at: new Date().toISOString()
      },
      {
        id: 3,
        employee_id: employeeId || "EMP-001",
        doc_type: "REPORT",
        title: "Отчёт о командировке (Самара)",
        doc_date: "2023-09-20",
        created_at: new Date().toISOString()
      }
    ];

    res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
    res.end(JSON.stringify(docs));
    return;
  }

  if (pathname === '/api/v1/dossier/reports' && req.method === 'POST') {
    let body = '';
    req.on('data', chunk => { body += chunk; });
    req.on('end', () => {
      let parsedBody = {};
      try { parsedBody = JSON.parse(body); } catch(e) {}

      const responsePayload = {
        id: 101,
        employee_id: parsedBody.employee_id || "EMP-001",
        template_type: parsedBody.template_type || "SUMMARY_STANDARD",
        status: "COMPLETED",
        summary_text: "✓ Итоговая справка успешно сформирована.",
        document_count: parsedBody.document_ids ? parsedBody.document_ids.length : 3,
        download_url: "/api/v1/dossier/reports/101/download",
        created_at: new Date().toISOString()
      };

      res.writeHead(201, { 'Content-Type': 'application/json; charset=utf-8' });
      res.end(JSON.stringify(responsePayload));
    });
    return;
  }

  // Mock Catalog API endpoints
  if (pathname === '/api/v1/documents/1/download' || (pathname.startsWith('/api/v1/documents/') && pathname.endsWith('/download'))) {
    res.writeHead(200, {
      'Content-Type': 'application/pdf',
      'Content-Disposition': 'attachment; filename="salmonella_outbreak.pdf"',
    });
    res.end('Содержимое документа: Протокол эпидемиологического расследования вспышки сальмонеллеза');
    return;
  }

  if (pathname.startsWith('/api/v1/documents/search')) {
    const query = parsedUrl.searchParams.get('query') || parsedUrl.searchParams.get('q') || '';
    const docs = [
      {
        id: '1',
        title: 'Протокол эпидемиологического расследования вспышки сальмонеллеза',
        author: 'НИИ Эпидемиологии',
        authorOrganization: 'НИИ Эпидемиологии',
        year: 2023,
        publicationYear: 2023,
        docType: 'Протокол расследования',
        fileName: 'salmonella_outbreak.pdf',
        fileSize: '2.4 МБ',
        description: 'Оперативный отчет и результаты бактериологического исследования.'
      },
      {
        id: '2',
        title: 'Отчет эпиднадзора по гриппу и ОРВИ за сезон 2022-2023',
        author: 'Центр мониторинга инфекций',
        authorOrganization: 'Центр мониторинга инфекций',
        year: 2023,
        publicationYear: 2023,
        docType: 'Отчёт эпиднадзора',
        fileName: 'flu_surveillance.pdf',
        fileSize: '4.1 МБ',
        description: 'Статистика заболеваемости и результаты лабораторного мониторинга.'
      }
    ];

    const filtered = query
      ? docs.filter(d => d.title.toLowerCase().includes(query.toLowerCase()) || d.description.toLowerCase().includes(query.toLowerCase()))
      : docs;

    res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
    res.end(JSON.stringify(filtered));
    return;
  }

  // Static file serving
  let relativePath = pathname === '/' ? 'index.html' : pathname.replace(/^\//, '');
  let filePath = path.join(__dirname, relativePath);

  if (!fs.existsSync(filePath) || fs.statSync(filePath).isDirectory()) {
    // Try in __dirname or fallback to 404
    if (fs.existsSync(path.join(__dirname, 'index.html'))) {
      filePath = path.join(__dirname, 'index.html');
    } else {
      res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
      res.end('Not Found');
      return;
    }
  }

  const ext = path.extname(filePath).toLowerCase();
  const mimeType = MIME_TYPES[ext] || 'application/octet-stream';

  try {
    const data = fs.readFileSync(filePath);
    res.writeHead(200, { 'Content-Type': mimeType });
    res.end(data);
  } catch (err) {
    res.writeHead(500, { 'Content-Type': 'text/plain; charset=utf-8' });
    res.end('Internal Server Error');
  }
});

server.listen(PORT, () => {
  console.log(`Server listening on http://127.0.0.1:${PORT}`);
});
