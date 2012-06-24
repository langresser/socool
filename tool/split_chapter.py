import re,os,struct,glob;

def is_juan_title(line):
    if re.search('.*第[壹贰叁肆伍陆柒捌玖拾佰零一二三四五六七八九十百0-9]+卷', line):
        return True;

    return False;
def is_chapter_title(line):
    #无限恐怖
    if re.search('.*半部.*第.*集.*第.*章', line):
        return True;

    #明朝那些事儿
    if re.search('.*第[壹贰叁肆伍陆柒捌玖拾佰零一二三四五六七八九十百0-9]+章', line):
        return True;

    if re.search('\s*前言\s*$', line):
        return True;
    if re.search('\s*引子\s*$', line):
        return True;
    if re.search('朱.{1,2}篇$', line):
        return True;
    if re.search('^后记$', line):
        return True;

    return False;
def get_juan_chapter(each):
    if False:
        title_line = each[0][each[0].find('第') :].strip();
        first_di = title_line.find('第');
        second_di = title_line.find('第', first_di + 1);
        juan = title_line[:second_di];
        zhang = title_line[title_line.rfind('第'):];
    else:
        title_line = each[0].strip().split('@@');

        juan = title_line[0];
        zhang = title_line[1];

    return juan,zhang
def split_file(file):
    fp = open(file, 'r');
    dir_name = file[:file.rfind('.')];

    if not os.path.exists(dir_name):
        os.makedirs(dir_name);

    all_text = [];
    data_text = [];

    current_juan = "";
    for line in fp:
        if line == '\n':
            continue;

        if is_juan_title(line):
            current_juan = line.strip();
            continue;

        if is_chapter_title(line):
            if len(data_text) > 0:
                all_text.append(data_text);

            data_text = [];
            data_text.append(current_juan + '@@' + line);
            continue;

        data_text.append(line);
    fp.close();
    if len(data_text) > 0:
        all_text.append(data_text);

    fpwd = open('{0}/chapter.txt'.format(dir_name), 'w');
    fpwdata = open('{0}/data_read.txt'.format(dir_name), 'w');


    i = -1;
    current_juan = "";
    paragra_data = [];

    for each in all_text:
        i += 1;
        juan,zhang = get_juan_chapter(each)
        
        each[0] = zhang + '\n';
  
        if each[len(each) - 1] == '\n':
            each.pop();
        size = 0;
        paragraph = 0;
        paragra_data.append(0);
        fpwdata.write('c{0}  {1}\n'.format(i, zhang));
        for eee in each:
            textLen = len(eee);
            fpwdata.write('{0}\n'.format(textLen));

            if textLen >=512:
                print(eee);

            if textLen >= 255:
                paragra_data.append(255);
                if textLen - 255 >= 255:
                    paragra_data.append(255);
                    paragra_data.append(textLen - 255 - 255);
                else:
                    paragra_data.append(textLen - 255);
            else:
                paragra_data.append(textLen);
            
            size += textLen;
            paragraph += 1;

        if juan != current_juan:
            current_juan = juan;
            fpwd.write('j{0}\n'.format(juan));
            fpwd.write('{0}@@{1}@@{2}\n'.format(len(each), size, zhang));
        else:
            fpwd.write('{0}@@{1}@@{2}\n'.format(len(each), size, zhang));

        fpw = open('{0}/{1}.txt'.format(dir_name, i), 'w');
        fpw.writelines(each);
        fpw.close();

    fpwd.close();
    fpwdata.close();

    fpws = open('{0}/data.db'.format(dir_name), 'wb');
    for each in paragra_data:
        bytes = struct.pack('B', each);
        fpws.write(bytes);
    fpws.close();


file_list = glob.glob('*.TXT')
for file in file_list:
    split_file(file);
