# Luồng branch lên VM và build APK

Workflow mới: `.github/workflows/branch_pipeline_to_vm_and_apk.yml`

## Luồng chạy

Khi `push` lên các branch:
- `feature/**`
- `bugfix/**`
- `hotfix/**`
- `release/**`

pipeline sẽ tự chạy theo thứ tự:

1. Resolve branch và commit SHA cần xử lý
2. Verify backend
   - `mvn -q -DskipTests package`
   - `mvn test -Dtest="FE*Test"`
3. Verify frontend
   - `npm ci`
   - `npm run lint --if-present`
   - `npm test -- --run --passWithNoTests`
   - `npm run build`
4. Deploy branch đó lên VM
   - checkout đúng branch trên `/opt/SLIB`
   - build lại frontend trên VM rồi sync sang `/var/www/slib/`
   - `docker compose up -d --build slib-backend slib-ai-service`
   - check health backend và AI
5. Xác nhận VM đã ở đúng commit vừa push
6. Build mobile APK
   - `flutter pub get`
   - `flutter test`
   - `flutter build apk --release`
7. Upload file APK làm artifact trên GitHub Actions

## Cách dùng

### Cách chuẩn

```bash
git add .
git commit -m "..."
git push origin <ten-branch>
```

Sau khi push:
- workflow sẽ tự chạy
- nếu verify fail thì dừng, không deploy VM
- nếu deploy xong mà VM chưa đúng commit thì fail
- nếu mọi thứ ổn, artifact APK sẽ xuất hiện trong run đó

### Chạy tay

Có thể chạy tay bằng workflow dispatch:
- vào GitHub Actions
- chọn `Branch Pipeline To VM And APK`
- nhập tên branch cần chạy

## Secret đang dùng

- `TAILSCALE_AUTHKEY`
- `VPS_SSH_KEY`
- `VPS_TAILSCALE_HOST`
- `VPS_USER`

## Ghi chú

- Branch trên VM vẫn là `vm-feature-deploy`, nhưng commit `HEAD` sẽ được đối chiếu đúng với commit vừa push.
- Workflow deploy branch cũ `Deploy Branch To VM` vẫn còn dùng được cho trường hợp cần deploy tay.
