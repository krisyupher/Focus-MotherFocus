# FocusMotherFocus API

FastAPI backend server for cross-platform mobile and desktop clients.

## Quick Start

```bash
# Install dependencies
pip install -r requirements.txt

# Run development server
python main.py

# Or use uvicorn directly
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

## API Documentation

Once running, visit:
- **Swagger UI**: http://localhost:8000/docs
- **ReDoc**: http://localhost:8000/redoc

## Endpoints

### Monitoring
- `GET /api/v1/monitoring/targets` - Get monitoring targets
- `POST /api/v1/monitoring/targets` - Add monitoring target
- `DELETE /api/v1/monitoring/targets/{id}` - Remove target
- `POST /api/v1/monitoring/sessions/start` - Start monitoring
- `POST /api/v1/monitoring/sessions/{id}/stop` - Stop monitoring

### Agreements
- `GET /api/v1/agreements` - Get user agreements
- `POST /api/v1/agreements` - Create agreement
- `GET /api/v1/agreements/{id}` - Get agreement details
- `POST /api/v1/agreements/{id}/complete` - Complete agreement
- `DELETE /api/v1/agreements/{id}` - Cancel agreement

### Avatar
- `POST /api/v1/avatar/intervention` - Trigger intervention
- `POST /api/v1/avatar/negotiate` - Negotiate time
- `GET /api/v1/avatar/status` - Get avatar status

## Environment Variables

Create a `.env` file:

```env
API_HOST=0.0.0.0
API_PORT=8000
DATABASE_URL=sqlite:///./focusmother.db
SECRET_KEY=your-secret-key-here
```

## Production Deployment

```bash
# Using gunicorn
pip install gunicorn
gunicorn main:app -w 4 -k uvicorn.workers.UvicornWorker --bind 0.0.0.0:8000

# Using Docker
docker build -t focusmother-api .
docker run -p 8000:8000 focusmother-api
```
