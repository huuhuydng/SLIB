import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:slib/views/home/widgets/section_title.dart';

void main() {
  testWidgets('SectionTitle renders provided text', (WidgetTester tester) async {
    await tester.pumpWidget(
      const MaterialApp(
        home: Scaffold(
          body: SectionTitle('Lịch trình của bạn'),
        ),
      ),
    );

    expect(find.text('Lịch trình của bạn'), findsOneWidget);

    final text = tester.widget<Text>(find.text('Lịch trình của bạn'));
    expect(text.style?.fontWeight, FontWeight.bold);
  });
}
