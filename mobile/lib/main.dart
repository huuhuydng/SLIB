import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:provider/provider.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:slib/services/booking_service.dart';
import 'package:slib/services/library_status_service.dart';
import 'package:slib/services/notification_service.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'firebase_options.dart';

// Import cac file cua ban
import 'services/auth_service.dart';
import 'main_screen.dart'; 
import 'views/authentication/on_boarding_screen.dart';

@pragma('vm:entry-point')
Future<void> _firebaseMessagingBackgroundHandler(RemoteMessage message) async {
  // If you're going to use other Firebase services in the background, such as Firestore,
  // make sure you call `initializeApp` before using other Firebase services.
  if (Firebase.apps.isEmpty) {
    await Firebase.initializeApp(options: DefaultFirebaseOptions.currentPlatform);
  }
  print("Handling a background message: ${message.messageId}");
  
  // CHAT_MESSAGE: Android tự hiện notification từ FCM notification payload
  // → không cần showBackgroundNotification (tránh duplicate)
  final type = message.data['type'] ?? '';
  if (type == 'CHAT_MESSAGE') {
    print('[BG] Skipping CHAT_MESSAGE (auto-displayed by Android)');
    return;
  }
  
  // Show local notification using our helper
  await showBackgroundNotification(message);
}

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // Initialize Vietnamese locale for date formatting
  await initializeDateFormatting('vi', null);

  try {
    if (Firebase.apps.isEmpty) {
      await Firebase.initializeApp(
        options: DefaultFirebaseOptions.currentPlatform,
      );
    } else {
      Firebase.app(); 
    }
  } catch (e) {
    print("Firebase init warning: $e");
  }

  // Set the background messaging handler early on, as a top-level function
  FirebaseMessaging.onBackgroundMessage(_firebaseMessagingBackgroundHandler);

  // Create AuthService first so NotificationService can use it
  final authService = AuthService();

  runApp(
    MultiProvider(
      providers: [
        ChangeNotifierProvider.value(value: authService),
        Provider<BookingService>(create: (_) => BookingService()),
        ChangeNotifierProvider(
          create: (_) => NotificationService(authService),
        ),
        ChangeNotifierProvider(
          create: (_) => LibraryStatusService(authService),
        ),
      ],
      child: const MyApp(),
    ),
  );
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'SLIB App',
      // Add localization delegates for DatePicker, TimePicker, etc.
      localizationsDelegates: const [
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      supportedLocales: const [
        Locale('vi', 'VN'),
        Locale('en', 'US'),
      ],
      locale: const Locale('vi', 'VN'),
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFFFF751F)),
        useMaterial3: true,
      ),
      home: const MyHomePage(),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key});

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _handleNavigation());
  }

  Future<void> _handleNavigation() async {
    try {
      final AuthService authService = context.read<AuthService>();
      final NotificationService notificationService = context.read<NotificationService>();
      final LibraryStatusService libraryStatusService = context.read<LibraryStatusService>();
      
      final results = await Future.wait([
        Future.delayed(const Duration(seconds: 2)),
        authService.checkLoginStatus(),
      ]);

      final bool isLoggedIn = results[1] as bool;

      // Initialize services if logged in (song song, không block navigation)
      if (isLoggedIn) {
        notificationService.initialize();
        libraryStatusService.initialize();
      }

      if (!mounted) return;

      if (isLoggedIn) {
        Navigator.pushReplacement(
          context, 
          MaterialPageRoute(builder: (_) => MainScreen(key: MainScreen.globalKey))
        );
      } else {
        Navigator.pushReplacement(
          context, 
          MaterialPageRoute(builder: (_) => const OnBoardingScreen())
        );
      }
    } catch (e) {
      print("Loi Navigation: $e");
      if (mounted) {
        Navigator.pushReplacement(
            context, MaterialPageRoute(builder: (_) => const OnBoardingScreen()));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Image.asset(
              'assets/images/logo.png',
              width: 150,
              height: 150,
              errorBuilder: (context, error, stackTrace) => const Icon(
                Icons.local_library_rounded, 
                size: 100, 
                color: Color(0xFFFF751F)
              ),
            ),
            const SizedBox(height: 20),
            // Loading
            const CircularProgressIndicator(color: Color(0xFFFF751F)),
          ],
        ),
      ),
    );
  }
}