import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:icons_plus/icons_plus.dart';


class OTPPage extends StatefulWidget{
  @override
  State<OTPPage> createState() => _OTPPageState();
}

class _OTPPageState extends State<OTPPage>{
  final List<TextEditingController> _otpControllers = List.generate(6, (_) => TextEditingController());
  @override
  void dispose() {
    // TODO: implement dispose
    _otpControllers.forEach((controller) => controller.dispose());
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
        appBar: AppBar(

        ),
      body: Padding(

        padding: const EdgeInsets.all(16.0),
        child: Column(
          children: [
            //title
            Text("Xác thực OTP",
            style: TextStyle(
              fontSize: 40,
              fontWeight: FontWeight.bold,
            ),
            ),
            SizedBox(height: 15,),

            Text("Nhập mã OTP đã được gửi đến email của bạn",
            style: TextStyle(
              fontSize: 16,
              color: Colors.black,
            ),

            )
          ],
        ),
      ));
  }

}